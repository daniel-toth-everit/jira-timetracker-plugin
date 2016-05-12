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
package org.everit.jira.reporting.plugin.query.util;

import org.everit.jira.querydsl.schema.QAppUser;
import org.everit.jira.querydsl.schema.QCwdDirectory;
import org.everit.jira.querydsl.schema.QCwdUser;
import org.everit.jira.querydsl.schema.QJiraissue;
import org.everit.jira.querydsl.schema.QProject;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;

/**
 * Helper class to Queries.
 */
public final class QueryUtil {

  /**
   * Create issue key String expression.
   */
  public static StringExpression createIssueKeyExpression(final QJiraissue qIssue,
      final QProject qProject) {
    StringExpression issueKey = qProject.pkey.concat("-").concat(qIssue.issuenum.stringValue());
    return issueKey;
  }

  // /**
  // * Create user StringExpression. In SQL return worklog.author if cwd_user.displayname is null
  // * otherwise cwd_user.displayname.
  // */
  // public static StringExpression createUserExpression(final QCwdUser qCwdUser,
  // final QWorklog qWorklog) {
  // StringExpression userExpression = new CaseBuilder()
  // .when(qCwdUser.displayName.isNull()).then(qWorklog.author)
  // .otherwise(qCwdUser.displayName);
  // return userExpression;
  // }

  /**
   * Select user displayName for issue assigne or report user.
   *
   * @param stringPath
   *          The StringPath of the issue para,
   */
  public static SQLQuery<String> selectDisplayNameForIssueUser(final StringPath stringPath) {
    QCwdUser qCwdUser = new QCwdUser("issueUser");
    QAppUser qAppUser = new QAppUser("appUserForIssue");
    QCwdDirectory qCwdDirectory = new QCwdDirectory("cwdDirectoryForIssue");
    // join cwd_directory dir on dir.id = usr.directory_id
    return SQLExpressions.select(new CaseBuilder()
        .when(qCwdUser.displayName.isNotNull()).then(qCwdUser.displayName)
        .otherwise(stringPath))
        .from(qCwdUser)
        .leftJoin(qAppUser).on(qCwdUser.lowerUserName.eq(qAppUser.lowerUserName))
        .leftJoin(qCwdDirectory).on(qCwdUser.directoryId.eq(qCwdDirectory.id))
        .where(qAppUser.userKey.eq(stringPath))
        .orderBy(qCwdDirectory.directoryPosition.asc())
        .limit(1L);
    // .and(qCwdDirectory.directoryPosition.eq(qCwdDirectory.directoryPosition.max()))
  }

  /**
   * TODO Select user displayName for issue assigne or report user.
   *
   * @param stringPath
   *          The StringPath of the issue para,
   */
  public static SQLQuery<String> selectDisplayNameForWorklogAuthor(final StringPath stringPath) {
    QCwdUser qCwdUser = new QCwdUser("issueUser");
    QAppUser qAppUser = new QAppUser("appUserForIssue");
    QCwdDirectory qCwdDirectory = new QCwdDirectory("cwdDirectoryForIssue");
    // join cwd_directory dir on dir.id = usr.directory_id
    return SQLExpressions.select(new CaseBuilder()
        .when(qCwdUser.displayName.isNotNull()).then(qCwdUser.displayName)
        .otherwise(stringPath))
        .from(qCwdUser)
        .leftJoin(qAppUser).on(qCwdUser.lowerUserName.eq(qAppUser.lowerUserName))
        .leftJoin(qCwdDirectory).on(qCwdUser.directoryId.eq(qCwdDirectory.id))
        .where(qAppUser.userKey.eq(stringPath))
        .orderBy(qCwdDirectory.directoryPosition.asc())
        .limit(1L);
    // .and(qCwdDirectory.directoryPosition.eq(qCwdDirectory.directoryPosition.max()))
  }

  private QueryUtil() {
  }
}
