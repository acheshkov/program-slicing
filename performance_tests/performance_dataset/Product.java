public boolean refresh()
throws AS400Exception,
           AS400SecurityException,
           ErrorCompletingRequestException,
           InterruptedException,
           IOException,
    ObjectDoesNotExistException {
    loadedLoadID_ = false;  // disregard any previously-loaded value
    // If no previous error with formats 0500 or 0100, get the format 0500 values.
    if (!error500_ && !error100_) refresh(500);
    // If no previous error with formats 0800 or 0100, get the format 0800 values.
    if (!error800_ && !error100_) refresh(800);
    // If there were errors with formats 0500 and 0800, and no errors with format 0100, get the format 0100 values.
    if (error500_ && error800_ && !error100_) refresh(100);
    // If no previous error with format 0500, get description text.
    if (!error500_) {
        loadedDescriptionText_ = false;
        getDescriptionText();
    }
    fillInOptionInformation();
    return !error100_;
}