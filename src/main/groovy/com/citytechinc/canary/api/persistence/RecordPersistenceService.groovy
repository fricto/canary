package com.citytechinc.canary.api.persistence

import com.citytechinc.canary.services.persistence.RecordHolder
import com.google.common.base.Optional

/**
 *
 * @author Josh Durbin, CITYTECH, Inc. 2013
 *
 * Copyright 2013 CITYTECH, Inc.
 *
 */
public interface RecordPersistenceService {

    /**
     *
     * @param recordHolder
     */
    void persistRecordHolder(RecordHolder recordHolder)

    /**
     *
     * @param canonicalMonitorName
     * @return
     */
    Optional<RecordHolder> getRecordHolder(String canonicalMonitorName)
}