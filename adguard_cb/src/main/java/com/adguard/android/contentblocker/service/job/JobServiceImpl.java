/*
 * This file is part of AdGuard Content Blocker (https://github.com/AdguardTeam/ContentBlocker).
 * Copyright © 2019 AdGuard Content Blocker. All rights reserved.
 * <p/>
 * AdGuard Content Blocker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * <p/>
 * AdGuard Content Blocker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * AdGuard Content Blocker.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.adguard.android.contentblocker.service.job;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.adguard.android.contentblocker.BuildConfig;
import com.adguard.android.contentblocker.ServiceLocator;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of {@link JobService}.
 */
public class JobServiceImpl implements JobService {
    private static final Logger LOG = LoggerFactory.getLogger(JobServiceImpl.class);

    private WeakReference<ServiceLocator> serviceLocatorRef;
    private WorkManager workManager;

    public JobServiceImpl(ServiceLocator serviceLocator, Context context) {
        this.serviceLocatorRef = new WeakReference<>(serviceLocator);
        this.workManager = WorkManager.getInstance(context);
    }

    @Override
    public void scheduleJobs(Id... ids) {
        ServiceLocator serviceLocator = serviceLocatorRef.get();
        if (ids == null || serviceLocator == null) {
            return;
        }
        String versionTag = BuildConfig.VERSION_NAME;
        for (Id id : ids) {
            Job job = JobFactory.getJob(serviceLocator, id);
            if (job == null) {
                LOG.warn("Job {} doesn't exist.", id);
                continue;
            }
            if (id != Id.UNKNOWN && !isJobPending(id) && canSchedule(job)) {
                LOG.info("Scheduling job for ID {}...", id.getTag());
                invokeWorkManagerSafe("Error while scheduling job", null, workManager -> {
                    workManager.enqueue(job.createWorkRequestBuilder().addTag(versionTag).build());
                    return null;
                });
            }
        }
    }

    @Override
    public void cancelJob(UUID uuid) {
        if (uuid == null) {
            return;
        }

        LOG.info("Cancelling job UUID {}...", uuid);
        invokeWorkManagerSafe("Error while canceling job", null, workManager -> {
            workManager.cancelWorkById(uuid);
            workManager.pruneWork();
            return null;
        });
    }

    @Override
    public void cancelJobs(Id... ids) {
        if (ids == null) {
            return;
        }

        for (Id id : ids) {
            String tag = id.getTag();
            LOG.info("Cancelling job ID {}...", tag);
            invokeWorkManagerSafe("Error while canceling jobs", null, workManager -> {
                workManager.cancelAllWorkByTag(tag);
                workManager.pruneWork();
                return null;
            });
        }
    }

    @Override
    public void cancelOldJobs() {
        ServiceLocator serviceLocator = serviceLocatorRef.get();
        if (serviceLocator == null) {
            return;
        }
        String versionTag = BuildConfig.VERSION_NAME;

        for (Id id : Id.values()) {
            deleteJobsWithoutTag(versionTag, workManager.getWorkInfosByTag(id.getTag()));
        }
    }

    @Override
    public boolean isJobPending(Id id) {
        return invokeWorkManagerSafe("Error while checking whether job is pending or not", false, workManager -> {
            try {
                return !workManager.getWorkInfosByTag(id.getTag()).get().isEmpty();
            } catch (ExecutionException | InterruptedException e) {
                LOG.warn("Error while checking whether job is pending or not", e);
                return false;
            }
        });
    }

    private void deleteJobsWithoutTag(@NonNull String tag, @NonNull ListenableFuture<List<WorkInfo>> future) {
        try {
            List<UUID> uuids = new ArrayList<>();
            for (WorkInfo info : future.get()) {
                boolean containsTag = false;
                for (String jobTag : info.getTags()) {
                    if (jobTag.equals(tag)) {
                        containsTag = true;
                        break;
                    }
                }
                if (!containsTag) {
                    uuids.add(info.getId());
                }
            }

            invokeWorkManagerSafe("Error while deleting old jobs", null, workManager -> {
                for (UUID uuid : uuids) {
                    workManager.cancelWorkById(uuid);
                }
                if (!uuids.isEmpty()) {
                    workManager.pruneWork();
                }
                return null;
            });
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Error while deleting jobs without tag", e);
        }
    }

    private boolean canSchedule(@NonNull Job job) {
        boolean state = job.canSchedule();
        LOG.info("Trying check job {} can schedule, state: {}", job.getId().getTag(), state);
        return state;
    }

    /**
     * Some information from the Android documentation:
     *      Always wrap a call to getAllPendingJobs(), schedule() and cancel() with a try catch as there
     *      are platform bugs with several OEMs in API 23, which cause this method to throw Exceptions.
     *      For reference: b/133556574, b/133556809, b/133556535
     * <p>
     * That means we should to wrap all methods' calls for WorkManager.
     *
     * @param logMessage message to be logged if an exception has been thrown
     * @param defaultValue value to be returned if an exception has been thrown
     * @param payload payload for [WorkManager] to be invoked safely
     */
    private <T> T invokeWorkManagerSafe(String logMessage, T defaultValue, WorkManagerPayload<T> payload) {
        try {
            return payload.invoke(workManager);
        } catch (Throwable th) {
            LOG.warn(logMessage, th);
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface WorkManagerPayload<T> {
        T invoke(WorkManager workManager);
    }
}