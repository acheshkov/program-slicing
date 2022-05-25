private void executeRequest() {
    DiskManagerRandomReadRequestImpl	request;
    synchronized( requests ) {
        if ( requests.isEmpty()) {
            return;
        }
        request = requests.remove( 0 );
    }
    if ( request.isCancelled()) {
        return;
    }
    DiskManagerFileInfoListener	info_listener	= null;
    org.gudy.azureus2.core3.disk.DiskManagerFileInfo core_file		= request.getFile().getCore();
    DownloadManager core_download = core_file.getDownloadManager();
    int			prev_hint_piece	= -1;
    int			curr_hint_piece = -1;
    try {
        if ( core_download.getTorrent() == null ) {
            throw( new DownloadException( "Torrent invalid" ));
        }
        if ( core_download.isDestroyed()) {
            Debug.out( "Download has been removed" );
            throw( new DownloadException( "Download has been removed" ));
        }
        TOTorrentFile	tf = core_file.getTorrentFile();
        TOTorrent 	torrent = tf.getTorrent();
        TOTorrentFile[]	tfs = torrent.getFiles();
        long	core_file_start_byte = 0;
        for (int i=0; i<core_file.getIndex(); i++) {
            core_file_start_byte += tfs[i].getLength();
        }
        long download_byte_start 	= core_file_start_byte + request.getOffset();
        long download_byte_end		= download_byte_start + request.getLength();
        if ( download_byte_end > core_file.getLength()) {
            throw( new DownloadException( "Request too large: file size=" + core_file.getLength() + ", request=" + download_byte_start + " -> " + download_byte_end ));
        }
        int piece_size	= (int)tf.getTorrent().getPieceLength();
        if ( core_file.getDownloaded() != core_file.getLength()) {
            if ( core_file.isSkipped()) {
                core_file.setSkipped( false );
            }
            boolean	force_start = download.isForceStart();
            if ( !force_start ) {
                download.setForceStart( true );
                set_force_start = true;
                final AESemaphore running_sem = new AESemaphore( "rs" );
                DownloadListener dl_listener =
                new DownloadListener() {
                    public void
                    stateChanged(
                        Download		download,
                        int				old_state,
                        int				new_state ) {
                        if ( new_state == Download.ST_DOWNLOADING || new_state == Download.ST_SEEDING ) {
                            running_sem.release();
                        }
                    }
                    public void
                    positionChanged(
                        Download	download,
                        int oldPosition,
                        int newPosition ) {
                    }
                };
                download.addListener( dl_listener );
                try {
                    if ( download.getState() != Download.ST_DOWNLOADING && download.getState() != Download.ST_SEEDING ) {
                        if ( !running_sem.reserve( 10*1000 )) {
                            throw( new DownloadException( "timeout waiting for download to start" ));
                        }
                    }
                } finally {
                    download.removeListener( dl_listener );
                }
            }
        }
        boolean	is_reverse = request.isReverse();
        final AESemaphore	wait_sem = new AESemaphore( "rr:waiter" );
        info_listener = new
        DiskManagerFileInfoListener() {
            public void
            dataWritten(
                long	offset,
                long	length ) {
                wait_sem.release();
            }
            public void
            dataChecked(
                long	offset,
                long	length ) {
            }
        };
        long 		start_time 		= SystemTime.getMonotonousTime();
        boolean		has_started		= false;
        core_file.addListener( info_listener );
        //System.out.println( "Request starts" );
        while( download_byte_start < download_byte_end ) {
            if ( request.isCancelled()) {
                throw( new Exception( "request cancelled" ));
            }
            //System.out.println( "Request current: " + download_byte_start + " -> " + download_byte_end );
            long	now = SystemTime.getMonotonousTime();
            int	piece_start 		= (int)( download_byte_start / piece_size );
            int	piece_start_offset	= (int)( download_byte_start % piece_size );
            int	piece_end	 		= (int)( ( download_byte_end - 1 ) / piece_size );
            int	piece_end_offset 	= (int)( ( download_byte_end - 1 ) % piece_size ) + 1;
            //System.out.println( "    piece details: " + piece_start + "/" + piece_start_offset + " -> " + piece_end + "/" + piece_end_offset );
            DiskManagerPiece[] pieces = null;
            DiskManager disk_manager = core_download.getDiskManager();
            if ( disk_manager != null ) {
                pieces = disk_manager.getPieces();
            }
            long	avail_start;
            long	avail_end;
            if ( pieces == null ) {
                if ( core_file.getDownloaded() == core_file.getLength()) {
                    avail_start = download_byte_start;
                    avail_end	= download_byte_end;
                } else {
                    if ( now - start_time < 10000 && !has_started ) {
                        wait_sem.reserve( 250 );
                        continue;
                    }
                    throw( new Exception( "download stopped" ));
                }
            } else {
                has_started = true;
                if ( is_reverse ) {
                    long	min_done = download_byte_end;
                    for ( int i=piece_end; i>= piece_start; i-- ) {
                        int	p_start = i==piece_start?piece_start_offset:0;
                        int	p_end 	= i==piece_end?piece_end_offset:piece_size;
                        DiskManagerPiece piece = pieces[i];
                        boolean[] done = piece.getWritten();
                        if ( done == null ) {
                            if ( piece.isDone()) {
                                min_done = i*piece_size;
                                continue;
                            } else {
                                break;
                            }
                        }
                        int	block_size = piece.getBlockSize( 0 );
                        int	first_block = p_start/block_size;
                        int	last_block 	= (p_end-1)/block_size;
                        for ( int j=last_block; j>=first_block; j--) {
                            if ( done[j] ) {
                                min_done = i*piece_size + j*block_size;
                            } else {
                                break;
                            }
                        }
                    }
                    avail_start = Math.max( download_byte_start, min_done );
                    avail_end	= download_byte_end;
                } else {
                    long	max_done = download_byte_start;
                    for ( int i=piece_start; i <= piece_end; i++ ) {
                        int	p_start = i==piece_start?piece_start_offset:0;
                        int	p_end 	= i==piece_end?piece_end_offset:piece_size;
                        DiskManagerPiece piece = pieces[i];
                        boolean[] done = piece.getWritten();
                        if ( done == null ) {
                            if ( piece.isDone()) {
                                max_done = (i+1)*piece_size;
                                continue;
                            } else {
                                break;
                            }
                        }
                        int	block_size = piece.getBlockSize( 0 );
                        int	first_block = p_start/block_size;
                        int	last_block 	= (p_end-1)/block_size;
                        for ( int j=first_block; j<=last_block; j++) {
                            if ( done[j] ) {
                                max_done = i*piece_size + (j+1)*block_size;
                            } else {
                                break;
                            }
                        }
                    }
                    avail_start = download_byte_start;
                    avail_end	= Math.min( download_byte_end, max_done );
                }
            }
            //System.out.println( "    avail: " + avail_start + " -> " + avail_end );
            int max_chunk = 128*1024;
            if ( avail_end > avail_start ) {
                long length = avail_end - avail_start;
                if ( length > max_chunk ) {
                    if ( is_reverse ) {
                        avail_start = avail_end - max_chunk;
                    } else {
                        avail_end	= avail_start + max_chunk;
                    }
                }
                //System.out.println( "got data: " + avail_start + " -> " + avail_end );
                long	read_offset = avail_start - core_file_start_byte;
                int		read_length	= (int)( avail_end - avail_start );
                DirectByteBuffer buffer = core_file.read( read_offset, read_length );
                request.dataAvailable( buffer, read_offset, read_length );
                if ( is_reverse ) {
                    download_byte_end = avail_start;
                } else {
                    download_byte_start = avail_end;
                }
                continue;
            }
            PEPeerManager pm = core_download.getPeerManager();
            if ( pm == null ) {
                if ( now - start_time < 10000 && !has_started ) {
                    wait_sem.reserve( 250 );
                    continue;
                }
                throw( new Exception( "download stopped" ));
            } else {
                has_started = true;
            }
            PiecePicker picker = pm.getPiecePicker();
            picker.setReverseBlockOrder( is_reverse );
            int	hint_piece;
            int	hint_offset;
            int	hint_length;
            if ( piece_start == piece_end ) {
                hint_piece	= piece_start;
                hint_offset = piece_start_offset;
                hint_length	= piece_end_offset - piece_start_offset;
            } else {
                if ( is_reverse ) {
                    hint_piece 	= piece_end;
                    hint_offset = 0;
                    hint_length	= piece_end_offset;
                } else {
                    hint_piece	= piece_start;
                    hint_offset = piece_start_offset;
                    hint_length	= piece_size - piece_start_offset;
                }
            }
            if ( curr_hint_piece == -1 ) {
                int[] existing = picker.getGlobalRequestHint();
                if ( existing != null ) {
                    curr_hint_piece = existing[0];
                }
            }
            //System.out.println( "hint: " + hint_piece + "/" + hint_offset + "/" + hint_length + ": curr=" + curr_hint_piece + ", prev=" + prev_hint_piece );
            picker.setGlobalRequestHint( hint_piece, hint_offset, hint_length );
            if ( hint_piece != curr_hint_piece ) {
                prev_hint_piece = curr_hint_piece;
                curr_hint_piece = hint_piece;
            }
            if ( prev_hint_piece != -1  ) {
                clearHint( pm, prev_hint_piece );
            }
            wait_sem.reserve( 250 );
        }
    } catch( Throwable e ) {
        request.failed( e );
    } finally {
        PEPeerManager pm = core_download.getPeerManager();
        if ( pm != null ) {
            PiecePicker picker = pm.getPiecePicker();
            if ( picker != null ) {
                picker.setReverseBlockOrder( false );
                picker.setGlobalRequestHint( -1, 0, 0 );
                if ( curr_hint_piece != -1  ) {
                    clearHint( pm, curr_hint_piece );
                }
            }
        }
        if ( info_listener != null ) {
            core_file.removeListener( info_listener );
        }
    }
}