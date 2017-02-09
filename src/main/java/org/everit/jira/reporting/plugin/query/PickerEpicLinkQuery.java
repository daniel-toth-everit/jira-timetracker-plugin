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
package org.everit.jira.reporting.plugin.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.everit.jira.querydsl.schema.QCustomfield;
import org.everit.jira.querydsl.schema.QCustomfieldvalue;
import org.everit.jira.querydsl.schema.QIssuelink;
import org.everit.jira.querydsl.schema.QIssuelinktype;
import org.everit.jira.querydsl.schema.QJiraissue;
import org.everit.jira.querydsl.schema.QProject;
import org.everit.jira.querydsl.support.QuerydslCallable;
import org.everit.jira.reporting.plugin.dto.PickerEpicLinkDTO;
import org.everit.jira.reporting.plugin.query.util.QueryUtil;

import com.querydsl.core.types.Projections;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;

/**
 * Query for gets epic links to picker.
 */
public class PickerEpicLinkQuery implements QuerydslCallable<List<PickerEpicLinkDTO>> {

  private QCustomfield qCustomfield;

  private QCustomfieldvalue qCustomfieldValue;

  private QJiraissue qIssue;

  private QIssuelink qIssuelink;

  private QIssuelinktype qIssuelinktype;

  private QProject qProject;

  private QJiraissue qSubIssue;

  /**
   * Simple constructor.
   */
  public PickerEpicLinkQuery() {
    qIssuelink = new QIssuelink("issuelink");
    qIssuelinktype = new QIssuelinktype("issuelinktype");
    qCustomfieldValue = new QCustomfieldvalue("customfieldvalue");
    qCustomfield = new QCustomfield("customfield");
    qIssue = new QJiraissue("issue");
    qProject = new QProject("project");
    qSubIssue = new QJiraissue("s_issue");
  }

  @Override
  public List<PickerEpicLinkDTO> call(final Connection connection,
      final Configuration configuration)
          throws SQLException {

    List<PickerEpicLinkDTO> result = new SQLQuery<PickerEpicLinkDTO>(connection, configuration)
        .select(Projections.bean(PickerEpicLinkDTO.class,
            qIssue.id.as(PickerEpicLinkDTO.AliasNames.EPIC_LINK_ID),
            qCustomfieldValue.stringvalue.as(PickerEpicLinkDTO.AliasNames.EPIC_NAME),
            QueryUtil.createIssueKeyExpression(qIssue, qProject)
                .as(PickerEpicLinkDTO.AliasNames.ISSUE_KEY)))
        .from(qIssue)
        .innerJoin(qProject).on(qProject.id.eq(qIssue.project))
        .innerJoin(qCustomfieldValue).on(qCustomfieldValue.issue.eq(qIssue.id))
        .innerJoin(qCustomfield).on(qCustomfield.id.eq(qCustomfieldValue.customfield)
            .and(qCustomfield.cfname.eq("Epic Name")))
        .where(SQLExpressions.select(qSubIssue.id)
            .from(qSubIssue)
            .innerJoin(qIssuelink).on(qSubIssue.id.eq(qIssuelink.source))
            .innerJoin(qIssuelinktype).on(qIssuelinktype.id.eq(qIssuelink.linktype)
                .and(qIssuelinktype.linkname.eq("Epic-Story Link")))
            .where(qSubIssue.id.eq(qIssue.id))
            .exists())
        .orderBy(qCustomfieldValue.stringvalue.asc())
        .fetch();

    return result;
  }

}
