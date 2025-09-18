package com.Gabou.sereneseasonsplus.storage;

public enum Priority {
    URGENT,     // must process immediately (e.g., snow chunks in summer)
    ACCELERATED, // process quickly but not blocking
    GRADUAL     // normal background processing
}
