/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.jira.core;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.everit.jira.core.impl.DateTimeServer;
import org.everit.jira.reporting.plugin.dto.MissingsWorklogsDTO;
import org.everit.jira.settings.dto.TimeTrackerGlobalSettings;
import org.ofbiz.core.entity.GenericEntityException;

/**
 * Provides common used methods for support about the plugin.
 */
public interface SupportManager {

  /**
   * Create a query and give back the list of dates where are no worklogs. The query examine the
   * days between the user creation date and the current date. The method not examine the weekends
   * and the properties file exclude dates but check the properties file include dates.
   *
   * @param from
   *          The query from parameter, the date in user's TimeZone.
   * @param to
   *          The query to parameter, the date in user's TimeZone.
   * @param workingHours
   *          The report have to check the spent time or not.
   * @param nonWorking
   *          Exclude or not the non-working issues.
   * @param settings
   *          TODO
   * @return The list of the MissingsWorklogsDTO.
   * @throws GenericEntityException
   *           If GenericEntity Exception.
   */
  List<MissingsWorklogsDTO> getDates(DateTimeServer from, DateTimeServer to, boolean workingHours,
      boolean nonWorking, TimeTrackerGlobalSettings settings) throws GenericEntityException;

  /**
   * Give back the Projects.
   *
   * @return whit Projects.
   */
  List<String> getProjectsId();

  /**
   * Give back the all worklogs spent time between the two date.
   *
   * @param startSummary
   *          The start date.
   * @param finishSummary
   *          The finish date.
   * @param issueIds
   *          The filtered issues ids. If null or empty then don't make filtered summary.
   * @return The summary spent time in seconds.
   */
  long summary(Date startSummary, Date finishSummary, List<Pattern> issueIds);
}
