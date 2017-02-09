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
package org.everit.jira.tests.core.impl.supportmanager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.everit.jira.core.SupportManager;
import org.everit.jira.core.impl.DateTimeServer;
import org.everit.jira.core.impl.SupportComponent;
import org.everit.jira.reporting.plugin.dto.MissingsWorklogsDTO;
import org.everit.jira.settings.dto.TimeTrackerGlobalSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.ofbiz.core.entity.EntityCondition;
import org.ofbiz.core.entity.EntityExpr;
import org.ofbiz.core.entity.EntityOperator;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.model.ModelEntity;

import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.MockApplicationUser;
import com.atlassian.jira.user.preferences.JiraUserPreferences;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.I18nHelper.BeanFactory;

public class GetDatesTest {

  static class DummyGenericValue extends GenericValue {
    private static final long serialVersionUID = 3415063923321743460L;

    private final Map<String, Object> values;

    @SuppressWarnings("deprecation")
    public DummyGenericValue(final Map<String, Object> values) {
      super(new ModelEntity());
      this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    @Override
    public Long getLong(final String key) {
      Object object = values.get(key);
      if (object == null) {
        return null;
      }
      return Long.valueOf(object.toString());
    }

    @Override
    public String getString(final String key) {
      Object object = values.get(key);
      if (object == null) {
        return null;
      }
      return String.valueOf(object);
    }
  }

  private static final String NOWORK_ISSUE_KEY = "NOWORK-1";

  private static final String WORK_ISSUE_KEY = "WORK-1";

  private SupportManager supportManager;

  private TimeTrackerGlobalSettings timeTrackerGlobalSettings;

  private DateTime today;

  private DateTime todayPlus1;

  private DateTime todayPlus2;

  private DateTime todayPlus3;

  private DateTime todayPlus4;

  private GenericValue createDummyGenericValue(final long issueId, final long timeworked) {
    HashMap<String, Object> values = new HashMap<>();
    values.put("issue", issueId);
    values.put("timeworked", timeworked);
    return new DummyGenericValue(values);

  }

  private void initMockComponentWorker() {
    timeTrackerGlobalSettings = new TimeTrackerGlobalSettings();
    DateTime date = new DateTime(1452124800000L, DateTimeZone.UTC); // 2016.01.07
    today = date.toDateTime(); // Thursday
    date = date.plusDays(1);
    todayPlus1 = date.toDateTime(); // excluded Friday
    date = date.plusDays(1);
    todayPlus2 = date.toDateTime();// included Staruday
    date = date.plusDays(1);
    todayPlus3 = date.toDateTime();// Sunday
    date = date.plusDays(1);
    todayPlus4 = date.toDateTime();// Monday

    timeTrackerGlobalSettings
        .excludeDates(new HashSet<>(Arrays.asList(1452211200000L))); // 2016.01.08 Friday
    timeTrackerGlobalSettings
        .includeDates(new HashSet<>(Arrays.asList(1452297600000L))); // 2016.01.09 Saturday
    timeTrackerGlobalSettings
        .filteredSummaryIssues(new ArrayList<>(Arrays.asList(Pattern.compile(NOWORK_ISSUE_KEY))));

    MockComponentWorker mockComponentWorker = new MockComponentWorker();

    JiraAuthenticationContext jiraAuthenticationContext =
        Mockito.mock(JiraAuthenticationContext.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(jiraAuthenticationContext.getLoggedInUser())
        .thenReturn(new MockApplicationUser("userkey", "username"));

    TimeTrackingConfiguration timeTrackingConfiguration =
        Mockito.mock(TimeTrackingConfiguration.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(timeTrackingConfiguration.getHoursPerDay())
        .thenReturn(new BigDecimal(1.0));

    PermissionManager permissionManager =
        Mockito.mock(PermissionManager.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(
        permissionManager.getProjects(Matchers.eq(Permissions.BROWSE),
            Matchers.any(ApplicationUser.class)))
        .thenReturn(new ArrayList<Project>());

    IssueManager issueManager = Mockito.mock(IssueManager.class, Mockito.RETURNS_DEEP_STUBS);
    MockIssue noworkIssue = new MockIssue(1, NOWORK_ISSUE_KEY);
    MockIssue workIssue = new MockIssue(2, WORK_ISSUE_KEY);
    Mockito.when(issueManager.getIssueObject(noworkIssue.getId()))
        .thenReturn(noworkIssue);
    Mockito.when(issueManager.getIssueObject(workIssue.getId()))
        .thenReturn(workIssue);

    OfBizDelegator ofBizDelegator = mockOfbizDelagator(noworkIssue, workIssue);

    supportManager = new SupportComponent(timeTrackingConfiguration);

    JiraUserPreferences mockJiraUserPreferences =
        Mockito.mock(JiraUserPreferences.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(mockJiraUserPreferences.getString("jira.user.timezone"))
        .thenReturn("UTC");

    UserPreferencesManager mockUserPreferencesManager =
        Mockito.mock(UserPreferencesManager.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(mockUserPreferencesManager.getPreferences(Matchers.any(ApplicationUser.class)))
        .thenReturn(mockJiraUserPreferences);
    mockComponentWorker.addMock(UserPreferencesManager.class, mockUserPreferencesManager);

    I18nHelper i18nHelper = Mockito.mock(I18nHelper.class, Mockito.RETURNS_DEEP_STUBS);
    BeanFactory mockBeanFactory = Mockito.mock(BeanFactory.class, Mockito.RETURNS_DEEP_STUBS);

    Mockito.when(mockBeanFactory.getInstance(Matchers.any(ApplicationUser.class)))
        .thenReturn(i18nHelper);

    Mockito.when(i18nHelper.getLocale())
        .thenReturn(Locale.ENGLISH);
    mockComponentWorker.addMock(I18nHelper.class, i18nHelper);
    mockComponentWorker.addMock(BeanFactory.class, mockBeanFactory);

    mockComponentWorker.addMock(JiraAuthenticationContext.class, jiraAuthenticationContext)
        .addMock(TimeTrackingConfiguration.class, timeTrackingConfiguration)
        .addMock(PermissionManager.class, permissionManager)
        .addMock(IssueManager.class, issueManager)
        .addMock(OfBizDelegator.class, ofBizDelegator)
        .init();

  }

  private OfBizDelegator mockOfbizDelagator(final MockIssue noworkIssue,
      final MockIssue workIssue) {
    OfBizDelegator ofBizDelegator = Mockito.mock(OfBizDelegator.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(ofBizDelegator.findByAnd(Matchers.anyString(),
        Matchers.argThat(new ArgumentMatcher<List<EntityCondition>>() {
          @Override
          public boolean matches(final Object argument) {
            if (argument == null) {
              return false;
            }
            List<EntityCondition> exprList = (List<EntityCondition>) argument;
            for (EntityCondition expression : exprList) {
              EntityExpr expr = (EntityExpr) expression;
              if ("startdate".equals(expr.getLhs())
                  && EntityOperator.GREATER_THAN_EQUAL_TO.equals(expr.getOperator())
                  && (today.getMillis() == ((Timestamp) expr.getRhs()).getTime())) {
                return true;
              }
            }
            return false;
          }
        })))
        .thenReturn(
            new ArrayList<>(Arrays.asList(createDummyGenericValue(workIssue.getId(), 3600L))));
    Mockito.when(ofBizDelegator.findByAnd(Matchers.anyString(),
        Matchers.argThat(new ArgumentMatcher<List<EntityCondition>>() {
          @Override
          public boolean matches(final Object argument) {
            if (argument == null) {
              return false;
            }
            List<EntityCondition> exprList = (List<EntityCondition>) argument;
            for (EntityCondition expression : exprList) {
              EntityExpr expr = (EntityExpr) expression;
              if ("startdate".equals(expr.getLhs())
                  && EntityOperator.GREATER_THAN_EQUAL_TO.equals(expr.getOperator())
                  && (todayPlus1.getMillis() == ((Timestamp) expr.getRhs()).getTime())) {
                return true;
              }
            }
            return false;
          }
        })))
        .thenReturn(new ArrayList<>(Arrays.asList(
            createDummyGenericValue(workIssue.getId(), 1000L),
            createDummyGenericValue(noworkIssue.getId(), 2000L)))); // not enough worklog
    Mockito.when(ofBizDelegator.findByAnd(Matchers.anyString(),
        Matchers.argThat(new ArgumentMatcher<List<EntityCondition>>() {
          @Override
          public boolean matches(final Object argument) {
            if (argument == null) {
              return false;
            }
            List<EntityCondition> exprList = (List<EntityCondition>) argument;
            for (EntityCondition expression : exprList) {
              EntityExpr expr = (EntityExpr) expression;
              if ("startdate".equals(expr.getLhs())
                  && EntityOperator.GREATER_THAN_EQUAL_TO.equals(expr.getOperator())
                  && (todayPlus2.getMillis() == ((Timestamp) expr.getRhs()).getTime())) {
                return true;
              }
            }
            return false;
          }
        })))
        .thenReturn(new ArrayList<>(Arrays.asList(
            createDummyGenericValue(noworkIssue.getId(), 2600L),
            createDummyGenericValue(workIssue.getId(), 1000L))));// enough but with non work issue
    Mockito.when(ofBizDelegator.findByAnd(Matchers.anyString(),
        Matchers.argThat(new ArgumentMatcher<List<EntityCondition>>() {
          @Override
          public boolean matches(final Object argument) {
            if (argument == null) {
              return false;
            }
            List<EntityCondition> exprList = (List<EntityCondition>) argument;
            for (EntityCondition expression : exprList) {
              EntityExpr expr = (EntityExpr) expression;
              if ("startdate".equals(expr.getLhs())
                  && EntityOperator.GREATER_THAN_EQUAL_TO.equals(expr.getOperator())
                  && (todayPlus3.getMillis() == ((Timestamp) expr.getRhs()).getTime())) {
                return true;
              }
            }
            return false;
          }
        })))
        .thenReturn(new ArrayList<>(Arrays.asList(
            createDummyGenericValue(workIssue.getId(), 500L),
            createDummyGenericValue(workIssue.getId(), 100L),
            createDummyGenericValue(workIssue.getId(), 2000L)))); // not enough worklog
    Mockito.when(ofBizDelegator.findByAnd(Matchers.anyString(),
        Matchers.argThat(new ArgumentMatcher<List<EntityCondition>>() {
          @Override
          public boolean matches(final Object argument) {
            if (argument == null) {
              return false;
            }
            List<EntityCondition> exprList = (List<EntityCondition>) argument;
            for (EntityCondition expression : exprList) {
              EntityExpr expr = (EntityExpr) expression;
              if ("startdate".equals(expr.getLhs())
                  && EntityOperator.GREATER_THAN_EQUAL_TO.equals(expr.getOperator())
                  && (todayPlus4.getMillis() == ((Timestamp) expr.getRhs()).getTime())) {
                return true;
              }
            }
            return false;
          }
        })))
        .thenReturn(null);
    return ofBizDelegator;
  }

  @Test
  public void testGetDates() throws GenericEntityException {
    initMockComponentWorker();
    DateTimeServer today = DateTimeServer.getInstanceBasedOnUserTimeZone(this.today);
    DateTimeServer todayPlus4 = DateTimeServer.getInstanceBasedOnUserTimeZone(this.todayPlus4);
    // DateTimeServer todayPlus3 = DateTimeServer.getInstanceBasedOnUserTimeZone(this.todayPlus3);
    DateTimeServer todayPlus2 = DateTimeServer.getInstanceBasedOnUserTimeZone(this.todayPlus2);

    List<MissingsWorklogsDTO> dates =
        supportManager.getDates(today, todayPlus4, false, false, timeTrackerGlobalSettings);
    Assert.assertEquals(1, dates.size());
    MissingsWorklogsDTO dto1 = dates.get(0);
    Assert.assertEquals("1", dto1.getHour());
    Assert.assertEquals(todayPlus4.getUserTimeZone().getMillis(), dto1.getDate().getTime());

    dates =
        supportManager.getDates(today, todayPlus4, false, true, timeTrackerGlobalSettings);
    Assert.assertEquals(1, dates.size());
    dto1 = dates.get(0);
    Assert.assertEquals("1", dto1.getHour());
    Assert.assertEquals(todayPlus4.getUserTimeZone().getMillis(), dto1.getDate().getTime());

    dates =
        supportManager.getDates(today, todayPlus4, true, false, timeTrackerGlobalSettings);
    DecimalFormat df = new DecimalFormat("#.#");
    Assert.assertEquals(1, dates.size());
    dto1 = dates.get(0);
    Assert.assertEquals("1", dto1.getHour());
    Assert.assertEquals(todayPlus4.getUserTimeZone().getMillis(), dto1.getDate().getTime());

    dates =
        supportManager.getDates(today, todayPlus4, true, true, timeTrackerGlobalSettings);
    Assert.assertEquals(2, dates.size());
    dto1 = dates.get(0);
    MissingsWorklogsDTO dto2 = dates.get(1);
    Assert.assertEquals("1", dto1.getHour());
    Assert.assertEquals(df.format(0.7), dto2.getHour());
    Assert.assertEquals(todayPlus4.getUserTimeZone().getMillis(), dto1.getDate().getTime());
    Assert.assertEquals(todayPlus2.getUserTimeZone().getMillis(), dto2.getDate().getTime());
  }
}
