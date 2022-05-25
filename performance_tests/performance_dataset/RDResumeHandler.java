public
void checkAllPieces(
    boolean newfiles) {
    //long	start = System.currentTimeMillis();
    DiskManagerRecheckInstance	recheck_inst = disk_manager.getRecheckScheduler().register( disk_manager, false );
    final AESemaphore	 run_sem = new AESemaphore( "RDResumeHandler::checkAllPieces:runsem", 2 );
    final List<DiskManagerCheckRequest>	failed_pieces = new ArrayList<DiskManagerCheckRequest>();
    try {
        boolean	resume_data_complete = false;
        try {
            check_in_progress	= true;
            boolean resumeEnabled = use_fast_resume;
            //disable fast resume if a new file was created
            if ( newfiles ) {
                resumeEnabled = false;
            }
            final AESemaphore	pending_checks_sem 	= new AESemaphore( "RD:PendingChecks" );
            int					pending_check_num	= 0;
            DiskManagerPiece[]	pieces	= disk_manager.getPieces();
            // calculate the current file sizes up front for performance reasons
            DiskManagerFileInfo[]	files = disk_manager.getFiles();
            Map	file_sizes = new HashMap();
            for (int i=0; i<files.length; i++) {
                try {
                    Long	len = new Long(((DiskManagerFileInfoImpl)files[i]).getCacheFile().getLength());
                    file_sizes.put( files[i], len );
                } catch( CacheFileManagerException e ) {
                    Debug.printStackTrace(e);
                }
            }
            if ( resumeEnabled ) {
                boolean resumeValid = false;
                byte[] resume_pieces = null;
                Map partialPieces = null;
                Map	resume_data = getResumeData();
                if ( resume_data != null ) {
                    try {
                        resume_pieces = (byte[])resume_data.get("resume data");
                        if ( resume_pieces != null ) {
                            if ( resume_pieces.length != pieces.length ) {
                                Debug.out( "Resume data array length mismatch: " + resume_pieces.length + "/" + pieces.length );
                                resume_pieces	= null;
                            }
                        }
                        partialPieces = (Map)resume_data.get("blocks");
                        resumeValid = ((Long)resume_data.get("valid")).intValue() == 1;
                        // if the torrent download is complete we don't need to invalidate the
                        // resume data
                        if ( isTorrentResumeDataComplete( disk_manager.getDownloadManager().getDownloadState(), resume_data )) {
                            resume_data_complete	= true;
                        } else {
                            // set it so that if we crash the NOT_DONE pieces will be
                            // rechecked
                            resume_data.put("valid", new Long(0));
                            saveResumeData( resume_data );
                        }
                    } catch(Exception ignore) {
                        // ignore.printStackTrace();
                    }
                }
                if ( resume_pieces == null ) {
                    check_is_full_check	= true;
                    resumeValid	= false;
                    resume_pieces	= new byte[pieces.length];
                    Arrays.fill( resume_pieces, PIECE_RECHECK_REQUIRED );
                }
                check_resume_was_valid = resumeValid;
                boolean	recheck_all	= use_fast_resume_recheck_all;
                if ( !recheck_all ) {
                    // override if not much left undone
                    long	total_not_done = 0;
                    int	piece_size = disk_manager.getPieceLength();
                    for (int i = 0; i < pieces.length; i++) {
                        if ( resume_pieces[i] != PIECE_DONE ) {
                            total_not_done	+= piece_size;
                        }
                    }
                    if ( total_not_done < 64*1024*1024 ) {
                        recheck_all	= true;
                    }
                }
                if (Logger.isEnabled()) {
                    int	total_not_done	= 0;
                    int	total_done		= 0;
                    int total_started	= 0;
                    int	total_recheck	= 0;
                    for (int i = 0; i < pieces.length; i++) {
                        byte	piece_state = resume_pieces[i];
                        if ( piece_state == PIECE_NOT_DONE ) {
                            total_not_done++;
                        } else if ( piece_state == PIECE_DONE ) {
                            total_done++;
                        } else if ( piece_state == PIECE_STARTED ) {
                            total_started++;
                        } else {
                            total_recheck++;
                        }
                    }
                    String	str = "valid=" + resumeValid + ",not done=" + total_not_done + ",done=" + total_done +
                                  ",started=" + total_started + ",recheck=" + total_recheck + ",rc all=" + recheck_all +
                                  ",full=" + check_is_full_check;
                    Logger.log(new LogEvent(disk_manager, LOGID, str ));
                }
                for (int i = 0; i < pieces.length; i++) {
                    check_position	= i;
                    DiskManagerPiece	dm_piece	= pieces[i];
                    disk_manager.setPercentDone(((i + 1) * 1000) / disk_manager.getNbPieces() );
                    boolean pieceCannotExist = false;
                    byte	piece_state = resume_pieces[i];
                    // valid resume data means that the resume array correctly represents
                    // the state of pieces on disk, be they done or not
                    if ( piece_state == PIECE_DONE || !resumeValid || recheck_all ) {
                        // at least check that file sizes are OK for this piece to be valid
                        DMPieceList list = disk_manager.getPieceList(i);
                        for (int j=0; j<list.size(); j++) {
                            DMPieceMapEntry	entry = list.get(j);
                            Long	file_size 		= (Long)file_sizes.get(entry.getFile());
                            if ( file_size == null ) {
                                piece_state	= PIECE_NOT_DONE;
                                pieceCannotExist = true;
                                if (Logger.isEnabled())
                                    Logger.log(new LogEvent(disk_manager, LOGID,
                                                            LogEvent.LT_WARNING, "Piece #" + i
                                                            + ": file is missing, " + "fails re-check."));
                                break;
                            }
                            long	expected_size 	= entry.getOffset() + entry.getLength();
                            if ( file_size.longValue() < expected_size ) {
                                piece_state	= PIECE_NOT_DONE;
                                pieceCannotExist = true;
                                if (Logger.isEnabled())
                                    Logger.log(new LogEvent(disk_manager, LOGID,
                                                            LogEvent.LT_WARNING, "Piece #" + i
                                                            + ": file is too small, fails re-check. File size = "
                                                            + file_size + ", piece needs " + expected_size));
                                break;
                            }
                        }
                    }
                    if ( piece_state == PIECE_DONE ) {
                        dm_piece.setDone( true );
                    } else if ( piece_state == PIECE_NOT_DONE && !recheck_all ) {
                        // if the piece isn't done and we haven't been asked to recheck all pieces
                        // on restart (only started pieces) then just set as not done
                    } else {
                        // We only need to recheck pieces that are marked as not-ok
                        // if the resume data is invalid or explicit recheck needed
                        if(pieceCannotExist) {
                            dm_piece.setDone( false );
                        } else if ( piece_state == PIECE_RECHECK_REQUIRED || !resumeValid ) {
                            run_sem.reserve();
                            while( !stopped ) {
                                if ( recheck_inst.getPermission()) {
                                    break;
                                }
                            }
                            if ( stopped ) {
                                break;
                            } else {
                                try {
                                    DiskManagerCheckRequest	request = disk_manager.createCheckRequest( i, null );
                                    request.setLowPriority( true );
                                    checker.enqueueCheckRequest(
                                        request,
                                    new DiskManagerCheckRequestListener() {
                                        public void
                                        checkCompleted(
                                            DiskManagerCheckRequest 	request,
                                            boolean						passed ) {
                                            if ( TEST_RECHECK_FAILURE_HANDLING && (int)(Math.random()*10) == 0 ) {
                                                disk_manager.getPiece(request.getPieceNumber()).setDone(false);
                                                passed  = false;
                                            }
                                            if ( !passed ) {
                                                synchronized( failed_pieces ) {
                                                    failed_pieces.add( request );
                                                }
                                            }
                                            complete();
                                        }
                                        public void
                                        checkCancelled(
                                            DiskManagerCheckRequest		request ) {
                                            complete();
                                        }
                                        public void
                                        checkFailed(
                                            DiskManagerCheckRequest 	request,
                                            Throwable		 			cause ) {
                                            complete();
                                        }
                                        protected void
                                        complete() {
                                            run_sem.release();
                                            pending_checks_sem.release();
                                        }
                                    });
                                    pending_check_num++;
                                } catch( Throwable e ) {
                                    Debug.printStackTrace(e);
                                }
                            }
                        }
                    }
                }
                while( pending_check_num > 0 ) {
                    pending_checks_sem.reserve();
                    pending_check_num--;
                }
                if ( partialPieces != null ) {
                    Iterator iter = partialPieces.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry key = (Map.Entry)iter.next();
                        int pieceNumber = Integer.parseInt((String)key.getKey());
                        DiskManagerPiece	dm_piece = pieces[ pieceNumber ];
                        if ( !dm_piece.isDone()) {
                            List blocks = (List)partialPieces.get(key.getKey());
                            Iterator iterBlock = blocks.iterator();
                            while (iterBlock.hasNext()) {
                                dm_piece.setWritten(((Long)iterBlock.next()).intValue());
                            }
                        }
                    }
                }
            } else {
                // resume not enabled, recheck everything
                for (int i = 0; i < pieces.length; i++) {
                    check_position	= i;
                    disk_manager.setPercentDone(((i + 1) * 1000) / disk_manager.getNbPieces() );
                    boolean pieceCannotExist = false;
                    // check if there is an underlying file for this piece, if not set it to not done
                    DMPieceList list = disk_manager.getPieceList(i);
                    for (int j=0; j<list.size(); j++) {
                        DMPieceMapEntry	entry = list.get(j);
                        Long	file_size 		= (Long)file_sizes.get(entry.getFile());
                        if ( file_size == null ) {
                            pieceCannotExist = true;
                            break;
                        }
                        long	expected_size 	= entry.getOffset() + entry.getLength();
                        if ( file_size.longValue() < expected_size ) {
                            pieceCannotExist = true;
                            break;
                        }
                    }
                    if(pieceCannotExist) {
                        disk_manager.getPiece(i).setDone(false);
                        continue;
                    }
                    run_sem.reserve();
                    while( ! stopped ) {
                        if ( recheck_inst.getPermission()) {
                            break;
                        }
                    }
                    if ( stopped ) {
                        break;
                    }
                    try {
                        DiskManagerCheckRequest	request = disk_manager.createCheckRequest( i, null );
                        request.setLowPriority( true );
                        checker.enqueueCheckRequest(
                            request,
                        new DiskManagerCheckRequestListener() {
                            public void
                            checkCompleted(
                                DiskManagerCheckRequest 	request,
                                boolean						passed ) {
                                if ( TEST_RECHECK_FAILURE_HANDLING && (int)(Math.random()*10) == 0 ) {
                                    disk_manager.getPiece(request.getPieceNumber()).setDone(false);
                                    passed  = false;
                                }
                                if ( !passed ) {
                                    synchronized( failed_pieces ) {
                                        failed_pieces.add( request );
                                    }
                                }
                                complete();
                            }
                            public void
                            checkCancelled(
                                DiskManagerCheckRequest		request ) {
                                complete();
                            }
                            public void
                            checkFailed(
                                DiskManagerCheckRequest 	request,
                                Throwable		 			cause ) {
                                complete();
                            }
                            protected void
                            complete() {
                                run_sem.release();
                                pending_checks_sem.release();
                            }
                        });
                        pending_check_num++;
                    } catch( Throwable e ) {
                        Debug.printStackTrace(e);
                    }
                }
                while( pending_check_num > 0 ) {
                    pending_checks_sem.reserve();
                    pending_check_num--;
                }
            }
            if ( failed_pieces.size() > 0 && !TEST_RECHECK_FAILURE_HANDLING ) {
                byte[][] piece_hashes = disk_manager.getTorrent().getPieces();
                ByteArrayHashMap<Integer>	hash_map = new ByteArrayHashMap<Integer>();
                for ( int i=0; i<piece_hashes.length; i++) {
                    hash_map.put( piece_hashes[i], i );
                }
                for ( DiskManagerCheckRequest request: failed_pieces ) {
                    while( ! stopped ) {
                        if ( recheck_inst.getPermission()) {
                            break;
                        }
                    }
                    if ( stopped ) {
                        break;
                    }
                    byte[] hash = request.getHash();
                    if ( hash != null ) {
                        final Integer target_index = hash_map.get( hash );
                        int		current_index 	= request.getPieceNumber();
                        int		piece_size		= disk_manager.getPieceLength( current_index );
                        if ( 	target_index != null &&
                                target_index != current_index &&
                                disk_manager.getPieceLength( target_index ) == piece_size &&
                                !disk_manager.isDone( target_index )) {
                            final AESemaphore sem = new AESemaphore( "PieceReorder" );
                            disk_manager.enqueueReadRequest(
                                disk_manager.createReadRequest( current_index, 0, piece_size ),
                            new DiskManagerReadRequestListener() {
                                public void
                                readCompleted(
                                    DiskManagerReadRequest 	request,
                                    DirectByteBuffer 		data ) {
                                    try {
                                        disk_manager.enqueueWriteRequest(
                                            disk_manager.createWriteRequest( target_index, 0, data, null ),
                                        new DiskManagerWriteRequestListener() {
                                            public void
                                            writeCompleted(
                                                DiskManagerWriteRequest 	request ) {
                                                try {
                                                    DiskManagerCheckRequest	check_request = disk_manager.createCheckRequest( target_index, null );
                                                    check_request.setLowPriority( true );
                                                    checker.enqueueCheckRequest(
                                                        check_request,
                                                    new DiskManagerCheckRequestListener() {
                                                        public void
                                                        checkCompleted(
                                                            DiskManagerCheckRequest 	request,
                                                            boolean						passed ) {
                                                            sem.release();
                                                        }
                                                        public void
                                                        checkCancelled(
                                                            DiskManagerCheckRequest		request ) {
                                                            sem.release();
                                                        }
                                                        public void
                                                        checkFailed(
                                                            DiskManagerCheckRequest 	request,
                                                            Throwable		 			cause ) {
                                                            sem.release();
                                                        }
                                                    });
                                                } catch( Throwable e ) {
                                                    sem.release();
                                                }
                                            }
                                            public void
                                            writeFailed(
                                                DiskManagerWriteRequest 	request,
                                                Throwable		 			cause ) {
                                                sem.release();
                                            }
                                        });
                                    } catch( Throwable e ) {
                                        sem.release();
                                    }
                                }
                                public void
                                readFailed(
                                    DiskManagerReadRequest 	request,
                                    Throwable		 		cause ) {
                                    sem.release();
                                }
                                public int
                                getPriority() {
                                    return( -1 );
                                }
                                public void
                                requestExecuted(
                                    long 	bytes ) {
                                }
                            });
                            sem.reserve();
                        }
                    }
                }
            }
        } finally {
            check_in_progress	= false;
        }
        //dump the newly built resume data to the disk/torrent
        if ( !( stopped || resume_data_complete )) {
            try {
                saveResumeData( true );
            } catch( Exception e ) {
                Debug.out( "Failed to dump initial resume data to disk" );
                Debug.printStackTrace( e );
            }
        }
    } catch( Throwable e ) {
        // if something went wrong then log and continue.
        Debug.printStackTrace(e);
    } finally {
        recheck_inst.unregister();
        // System.out.println( "Check of '" + disk_manager.getDownloadManager().getDisplayName() + "' completed in " + (System.currentTimeMillis() - start));
    }
}