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

import java.text.ParseException;
import java.util.List;

import org.everit.jira.core.dto.WorklogParameter;
import org.everit.jira.core.impl.DateTimeServer;
import org.everit.jira.timetracker.plugin.dto.EveritWorklog;
import org.everit.jira.timetracker.plugin.exception.WorklogException;

import com.atlassian.jira.exception.DataAccessException;

/**
 * The EVWorklogManager is used to retrieve, create, edit, and remove work logs in JIRA.
 */
public interface EVWorklogManager {

  /**
   * Count worklog size without permission check between start and end date for the logged user.
   *
   * @param startDate
   *          the start date.
   * @param endDate
   *          the end date.
   * @return the size of worklogs.
   */
  long countWorklogsWithoutPermissionChecks(DateTimeServer startDate, DateTimeServer endDate);

  /**
   * Creates a worklog based on given parameters.
   *
   * @param worklogParameter
   *          the worklog information to create.
   *
   * @throws WorklogException
   *           if has fail to create worklog.
   */
  void createWorklog(WorklogParameter worklogParameter);

  /**
   * Deletes the worklog based on worklog id.
   *
   * @param worklogId
   *          The id of the worklog.
   * @param optionalValue
   *          the optional value of the remaining estimate type. Example: newEstimate or
   *          adjustEstimate value.
   * @param remainingEstimateType
   *          the type of the remaining estimate.
   *
   * @throws WorklogException
   *           if has fail to delete worklog.
   */
  void deleteWorklog(Long worklogId, final String optionalValue,
      final RemainingEstimateType remainingEstimateType);

  /**
   * Edit an existing worklog whit the given parameters.
   *
   * @param worklogId
   *          the id of the worklog.
   * @param worklogParameter
   *          the worklog information to edit.
   *
   * @throws WorklogException
   *           if has fail to edit worklog.
   */
  void editWorklog(final Long worklogId, final WorklogParameter worklogParameter);

  /**
   * Give back the Worklog based on worklog id.
   *
   * @param worklogId
   *          The id of the worklog.
   * @return The result {@link EveritWorklog}.
   *
   * @throws ParseException
   *           If cannot parse the worklog date.
   * @throws WorklogException
   *           if not found worklog.
   */
  EveritWorklog getWorklog(Long worklogId) throws ParseException, WorklogException;

  /**
   * Give back the days all worklog of the selectedUser. If selectedUser null or empty the actual
   * logged in user will used.
   *
   * @param startDate
   *          The date.
   * @param selectedUser
   *          The selected User.
   * @return The list of the date all worklogs.
   * @throws ParseException
   *           When can't parse the worklog date.
   */
  List<EveritWorklog> getWorklogs(String selectedUser, DateTimeServer startDate,
      DateTimeServer endDate)
      throws DataAccessException, ParseException;
}
