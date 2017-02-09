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
package org.everit.jira.timetracker.plugin.dto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.everit.jira.core.SupportManager;
import org.everit.jira.core.TimetrackerManager;
import org.everit.jira.core.impl.DateTimeServer;
import org.everit.jira.timetracker.plugin.DurationFormatter;
import org.everit.jira.timetracker.plugin.util.DateTimeConverterUtil;
import org.joda.time.DateTime;

import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;

/**
 * Information of daily, weekly and monthly summary.
 */
public final class SummaryDTO {

  /**
   * Builder class to create {@link SummaryDTO} object.
   */
  // TODO WRITE tests for this class
  public static class SummaryDTOBuilder {

    private static final long HUNDRED = 100;

    private static final int SECOND_IN_HOUR = 3600;

    private final DateTimeServer date;

    private double dayExpectedWorkSeconds;

    private Long dayFilteredSummaryInSecond;

    private long daySummaryInSeconds;

    private final DurationFormatter durationFormatter;

    private final Set<DateTime> excludeDatesAsSet;

    private final Set<DateTime> includeDatesAsSet;

    private final List<Pattern> issuePatterns;

    private double monthExpectedWorkSeconds;

    private Long monthFilteredSummaryInSecond;

    private long monthSummaryInSecounds;

    private final SupportManager supportManager;

    private final TimetrackerManager timetrackerManager;

    private final TimeTrackingConfiguration timeTrackingConfiguration;

    private double weekExpectedWorkSeconds;

    private Long weekFilteredSummaryInSecond;

    private long weekSummaryInSecond;

    /**
     * Simple constructor.
     *
     * @param timeTrackingConfiguration
     *          the {@link TimeTrackingConfiguration} instance.
     * @param timetrackerManager
     *          the {@link TimetrackerManager} instance.
     * @param supportManager
     *          the {@link SupportManager} instance.
     * @param date
     *          the date of selected day. We use the create day summary and we use to date of exime
     *          week and month.
     * @param excludeDatesAsSet
     *          the set of excludes dates from timetracker configuration.
     * @param includeDatesAsSet
     *          the set of include dates from timetracker configuration.
     * @param issuePatterns
     *          the list of issue patterns from timetracker configuration.
     */
    public SummaryDTOBuilder(final TimeTrackingConfiguration timeTrackingConfiguration,
        final TimetrackerManager timetrackerManager,
        final SupportManager supportManager,
        final DateTimeServer date,
        final Set<DateTime> excludeDatesAsSet,
        final Set<DateTime> includeDatesAsSet,
        final List<Pattern> issuePatterns) {
      durationFormatter = new DurationFormatter();
      this.timeTrackingConfiguration = timeTrackingConfiguration;
      this.supportManager = supportManager;
      this.timetrackerManager = timetrackerManager;
      this.excludeDatesAsSet = excludeDatesAsSet;
      this.includeDatesAsSet = includeDatesAsSet;
      this.issuePatterns = issuePatterns;
      this.date = date;
    }

    private double calculateDailyPercent(final long daySummaryInSeconds, final double hoursPerDay) {
      double daySumMin = daySummaryInSeconds / (double) DateTimeConverterUtil.SECONDS_PER_MINUTE;
      double daySumHour = daySumMin / DateTimeConverterUtil.MINUTES_PER_HOUR;

      return daySumHour / hoursPerDay;
    }

    private double calculateDayFilteredNonWorkIndicatorPrecent(
        final double dayFilteredRealWorkIndicatorPrecent, final Long dayFilteredSummaryInSecond,
        final long daySummaryInSeconds, final double expectedWorkSecondsInDay) {
      double dayFilteredNoneWorkIndicatorPrecent = 0;
      if (dayFilteredSummaryInSecond != null) {
        dayFilteredNoneWorkIndicatorPrecent =
            ((daySummaryInSeconds - dayFilteredSummaryInSecond) / expectedWorkSecondsInDay)
                * HUNDRED;
      } else {
        dayFilteredNoneWorkIndicatorPrecent =
            (daySummaryInSeconds / expectedWorkSecondsInDay) * HUNDRED;
      }

      dayFilteredNoneWorkIndicatorPrecent = correctNoneWorkIndicatorPercent(
          dayFilteredRealWorkIndicatorPrecent, dayFilteredNoneWorkIndicatorPrecent);
      return dayFilteredNoneWorkIndicatorPrecent;
    }

    private double calculateExpectedWorkSecondsInMonth(final double expectedWorkSecondsInDay) {
      Calendar dayIndex = createNewCalendarWithWeekStart();
      dayIndex.setTime(date.getUserTimeZoneDate());
      dayIndex.set(Calendar.DAY_OF_MONTH, 1);
      Calendar monthLastDay = createNewCalendarWithWeekStart();
      monthLastDay.setTime(date.getUserTimeZoneDate());
      monthLastDay.set(Calendar.DAY_OF_MONTH,
          monthLastDay.getActualMaximum(Calendar.DAY_OF_MONTH));

      long daysInMonth =
          TimeUnit.DAYS.convert(monthLastDay.getTimeInMillis() - dayIndex.getTimeInMillis(),
              TimeUnit.MILLISECONDS) + 1;
      int excludeDtaes =
          timetrackerManager.getExcludeDaysOfTheMonth(date.getUserTimeZone(), excludeDatesAsSet)
              .size();
      int includeDtaes =
          timetrackerManager.getIncludeDaysOfTheMonth(date.getUserTimeZone(), includeDatesAsSet)
              .size();
      int nonWorkDaysCount = 0;
      for (int i = 1; i <= daysInMonth; i++) {
        int dayOfweek = dayIndex.get(Calendar.DAY_OF_WEEK);
        if ((dayOfweek == Calendar.SUNDAY) || (dayOfweek == Calendar.SATURDAY)) {
          nonWorkDaysCount++;
        }
        dayIndex.add(Calendar.DAY_OF_MONTH, 1);
      }
      long realWorkDaysInMonth = (daysInMonth + includeDtaes) - excludeDtaes - nonWorkDaysCount;
      return realWorkDaysInMonth * expectedWorkSecondsInDay;
    }

    private double calculateExpectedWorkSecondsInWeek(final double expectedWorkSecondsInDay) {
      List<DateTime> weekdays = new ArrayList<>();
      Calendar dayIndex = createNewCalendarWithWeekStart();
      dayIndex.setTime(getWeekStart(date.getUserTimeZone().toDate()));
      for (int i = 0; i < DateTimeConverterUtil.DAYS_PER_WEEK; i++) {
        weekdays.add(new DateTime(dayIndex.getTimeInMillis(), date.getUserTimeZone().getZone()));
        dayIndex.add(Calendar.DAY_OF_MONTH, 1);
      }
      double realWorkDaysInWeek = timetrackerManager.countRealWorkDaysInWeek(weekdays,
          excludeDatesAsSet, includeDatesAsSet);
      return realWorkDaysInWeek * expectedWorkSecondsInDay;
    }

    private void calculateFilteredAndNotFilteredSummarySeconds(final DateTimeServer date,
        final List<Pattern> issuesRegex) {
      Calendar currentStartCalendar = createNewCalendarWithWeekStart();
      currentStartCalendar.setTime(date.getUserTimeZone().toDate());
      currentStartCalendar.setTimeZone(date.getUserTimeZone().getZone().toTimeZone());
      DateTime currentDayStart = DateTimeConverterUtil.setDateToDayStart(date.getUserTimeZone());

      // calculate day summary
      DateTime startDateTime = currentDayStart.toDateTime();
      DateTime endDateTime = startDateTime.plusDays(1);
      daySummaryInSeconds = supportManager.summary(
          startDateTime.toDate(), endDateTime.toDate(), null);
      if (isIssuePatternsNotEmpty()) {
        dayFilteredSummaryInSecond = supportManager.summary(
            startDateTime.toDate(), endDateTime.toDate(), issuesRegex);
      }
      // calculate weeksummary
      Calendar weekStart = (Calendar) currentStartCalendar.clone();
      while (weekStart.get(Calendar.DAY_OF_WEEK) != weekStart.getFirstDayOfWeek()) {
        weekStart.add(Calendar.DATE, -1); // Substract 1 day until first day of week.
      }
      startDateTime = new DateTime(weekStart.getTimeInMillis());
      endDateTime = startDateTime.plusDays(DateTimeConverterUtil.DAYS_PER_WEEK);

      weekSummaryInSecond =
          supportManager.summary(startDateTime.toDate(), endDateTime.toDate(), null);
      if (isIssuePatternsNotEmpty()) {
        weekFilteredSummaryInSecond = supportManager.summary(
            startDateTime.toDate(), endDateTime.toDate(), issuesRegex);
      }
      // calculate month summary
      startDateTime = currentDayStart.withDayOfMonth(1);

      Calendar monthEndCalendar = (Calendar) currentStartCalendar.clone();
      monthEndCalendar.set(Calendar.DAY_OF_MONTH,
          monthEndCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
      monthEndCalendar.add(Calendar.DAY_OF_MONTH, 1);
      endDateTime = new DateTime(monthEndCalendar.getTimeInMillis());
      monthSummaryInSecounds =
          supportManager.summary(startDateTime.toDate(), endDateTime.toDate(), null);
      if (isIssuePatternsNotEmpty()) {
        monthFilteredSummaryInSecond =
            supportManager.summary(startDateTime.toDate(), endDateTime.toDate(), issuesRegex);
      }
    }

    private String calculateFormattedNonWorkTimeInDay(final Long dayFilteredSummaryInSecond,
        final long daySummaryInSeconds) {
      if (dayFilteredSummaryInSecond == null) {
        return format(daySummaryInSeconds);
      }
      return format(daySummaryInSeconds - dayFilteredSummaryInSecond);
    }

    private String calculateFormattedNonWorkTimeInMonth(final Long monthFilteredSummaryInSecond,
        final long monthSummaryInSecounds) {
      if (monthFilteredSummaryInSecond == null) {
        return format(monthSummaryInSecounds);
      }
      return format(monthSummaryInSecounds - monthFilteredSummaryInSecond);
    }

    private String calculateFormattedNonWorkTimeInWeek(final Long weekFilteredSummaryInSecond,
        final long weekSummaryInSecond) {
      if (weekFilteredSummaryInSecond == null) {
        return format(weekSummaryInSecond);
      }
      return format(weekSummaryInSecond - weekFilteredSummaryInSecond);
    }

    private double calculateMonthFilteredNonWorkIndicatorPrecent(
        final double monthFilteredRealWorkIndicatorPrecent, final Long monthFilteredSummaryInSecond,
        final long monthSummaryInSecounds, final double expectedWorkSecondsInMonth) {
      double monthFilteredNonWorkIndicatorPrecent = 0;
      if (monthFilteredSummaryInSecond != null) {
        monthFilteredNonWorkIndicatorPrecent =
            ((monthSummaryInSecounds - monthFilteredSummaryInSecond) / expectedWorkSecondsInMonth)
                * HUNDRED;
      } else {
        monthFilteredNonWorkIndicatorPrecent =
            (monthSummaryInSecounds / expectedWorkSecondsInMonth) * HUNDRED;
      }
      monthFilteredNonWorkIndicatorPrecent = correctNoneWorkIndicatorPercent(
          monthFilteredRealWorkIndicatorPrecent, monthFilteredNonWorkIndicatorPrecent);
      return monthFilteredNonWorkIndicatorPrecent;
    }

    private double calculateWeekFilteredNonWorkIndicatorPrecent(
        final double weekFilteredRealWorkIndicatorPrecent, final Long weekFilteredSummaryInSecond,
        final long weekSummaryInSecond, final double expectedWorkSecondsInWeek) {
      double weekFilteredNonWorkIndicatorPrecent = 0;
      if (weekFilteredSummaryInSecond != null) {
        weekFilteredNonWorkIndicatorPrecent =
            ((weekSummaryInSecond - weekFilteredSummaryInSecond) / expectedWorkSecondsInWeek)
                * HUNDRED;
      } else {
        weekFilteredNonWorkIndicatorPrecent =
            (weekSummaryInSecond / expectedWorkSecondsInWeek) * HUNDRED;
      }
      weekFilteredNonWorkIndicatorPrecent = correctNoneWorkIndicatorPercent(
          weekFilteredRealWorkIndicatorPrecent, weekFilteredNonWorkIndicatorPrecent);
      return weekFilteredNonWorkIndicatorPrecent;
    }

    private double correctNoneWorkIndicatorPercent(final double realWorkIndicatorPrecent,
        final double noneWorkIndicatorPrecent) {
      double correctedNoneWorkIndicatorPercent = noneWorkIndicatorPrecent;
      double sumPercent = noneWorkIndicatorPrecent + realWorkIndicatorPrecent;
      if (sumPercent > HUNDRED) {
        correctedNoneWorkIndicatorPercent =
            noneWorkIndicatorPrecent - (sumPercent - HUNDRED);
      }
      return correctedNoneWorkIndicatorPercent;
    }

    private Calendar createNewCalendarWithWeekStart() {
      ApplicationProperties applicationProperties = ComponentAccessor.getApplicationProperties();
      boolean useISO8601 =
          applicationProperties.getOption(APKeys.JIRA_DATE_TIME_PICKER_USE_ISO8601);
      Calendar c = Calendar.getInstance();
      if (useISO8601) {
        c.setFirstDayOfWeek(Calendar.MONDAY);
      }
      return c;
    }

    /**
     * Creates {@link SummaryDTO} object.
     */
    public SummaryDTO createSummaryDTO() {
      calculateFilteredAndNotFilteredSummarySeconds(date, issuePatterns);

      double hoursPerDay = timeTrackingConfiguration.getHoursPerDay().doubleValue();
      dayExpectedWorkSeconds = hoursPerDay * SECOND_IN_HOUR;
      weekExpectedWorkSeconds = calculateExpectedWorkSecondsInWeek(dayExpectedWorkSeconds);
      monthExpectedWorkSeconds = calculateExpectedWorkSecondsInMonth(dayExpectedWorkSeconds);

      double dailyPercent = calculateDailyPercent(daySummaryInSeconds, hoursPerDay);

      double dayFilteredRealWorkIndicatorPrecent = 1;
      double dayFilteredNonWorkIndicatorPrecent = 1;
      String dayFilteredSummary = "";
      String formattedNonWorkTimeInDay = "";
      double weekFilteredRealWorkIndicatorPrecent = 1;
      double weekFilteredNonWorkIndicatorPrecent = 1;
      String weekFilteredSummary = "";
      String formattedNonWorkTimeInWeek = "";
      double monthFilteredRealWorkIndicatorPrecent = 1;
      double monthFilteredNonWorkIndicatorPrecent = 1;
      String monthFilteredSummary = "";
      String formattedNonWorkTimeInMonth = "";

      if (isIssuePatternsNotEmpty()) {
        dayFilteredRealWorkIndicatorPrecent =
            (dayFilteredSummaryInSecond / dayExpectedWorkSeconds) * HUNDRED;
        dayFilteredNonWorkIndicatorPrecent =
            calculateDayFilteredNonWorkIndicatorPrecent(dayFilteredRealWorkIndicatorPrecent,
                dayFilteredSummaryInSecond, daySummaryInSeconds, dayExpectedWorkSeconds);
        dayFilteredSummary = format(dayFilteredSummaryInSecond);
        formattedNonWorkTimeInDay =
            calculateFormattedNonWorkTimeInDay(dayFilteredSummaryInSecond, daySummaryInSeconds);

        weekFilteredRealWorkIndicatorPrecent =
            (weekFilteredSummaryInSecond / weekExpectedWorkSeconds) * HUNDRED;
        weekFilteredNonWorkIndicatorPrecent =
            calculateWeekFilteredNonWorkIndicatorPrecent(weekFilteredRealWorkIndicatorPrecent,
                weekFilteredSummaryInSecond, weekSummaryInSecond, weekExpectedWorkSeconds);
        weekFilteredSummary = format(weekFilteredSummaryInSecond);
        formattedNonWorkTimeInWeek =
            calculateFormattedNonWorkTimeInWeek(weekFilteredSummaryInSecond, weekSummaryInSecond);

        monthFilteredRealWorkIndicatorPrecent =
            (monthFilteredSummaryInSecond / monthExpectedWorkSeconds) * HUNDRED;
        monthFilteredNonWorkIndicatorPrecent =
            calculateMonthFilteredNonWorkIndicatorPrecent(monthFilteredRealWorkIndicatorPrecent,
                monthFilteredSummaryInSecond, monthSummaryInSecounds, monthExpectedWorkSeconds);
        monthFilteredSummary = format(monthFilteredSummaryInSecond);
        formattedNonWorkTimeInMonth = calculateFormattedNonWorkTimeInMonth(
            monthFilteredSummaryInSecond, monthSummaryInSecounds);

      }

      double dayFilteredPercent = daySummaryInSeconds / dayExpectedWorkSeconds;

      double dayIndicatorPrecent = (daySummaryInSeconds / dayExpectedWorkSeconds) * HUNDRED;

      String daySummary = format(daySummaryInSeconds);

      String formattedExpectedWorkTimeInDay = format((long) dayExpectedWorkSeconds);

      String formattedExpectedWorkTimeInWeek = format((long) weekExpectedWorkSeconds);

      String formattedExpectedWorkTimeInMonth = format((long) monthExpectedWorkSeconds);

      double monthFilteredPercent = monthSummaryInSecounds / monthExpectedWorkSeconds;

      double monthIndicatorPrecent =
          (monthSummaryInSecounds / monthExpectedWorkSeconds) * HUNDRED;

      String monthSummary = format(monthSummaryInSecounds);

      double weekFilteredPercent = weekSummaryInSecond / weekExpectedWorkSeconds;

      double weekIndicatorPrecent = (weekSummaryInSecond / weekExpectedWorkSeconds) * HUNDRED;

      String weekSummary = format(weekSummaryInSecond);

      String hoursPerDayFormatted = durationFormatter.workHoursDayIndustryDuration();

      String daySumIndustryFormatted = durationFormatter.industryDuration(daySummaryInSeconds);

      SummaryUnitDTO day = new SummaryUnitDTO()
          .filteredRealWorkIndicatorPrecent(dayFilteredRealWorkIndicatorPrecent)
          .filteredNonWorkIndicatorPrecent(dayFilteredNonWorkIndicatorPrecent)
          .indicatorPrecent(dayIndicatorPrecent)
          .filteredSummary(dayFilteredSummary)
          .summary(daySummary)
          .formattedExpectedWorkTime(formattedExpectedWorkTimeInDay)
          .formattedNonWorkTime(formattedNonWorkTimeInDay)
          .filteredPercent(dayFilteredPercent);

      SummaryUnitDTO week = new SummaryUnitDTO()
          .filteredRealWorkIndicatorPrecent(weekFilteredRealWorkIndicatorPrecent)
          .filteredNonWorkIndicatorPrecent(weekFilteredNonWorkIndicatorPrecent)
          .indicatorPrecent(weekIndicatorPrecent)
          .filteredSummary(weekFilteredSummary)
          .summary(weekSummary)
          .formattedExpectedWorkTime(formattedExpectedWorkTimeInWeek)
          .formattedNonWorkTime(formattedNonWorkTimeInWeek)
          .filteredPercent(weekFilteredPercent);

      SummaryUnitDTO month = new SummaryUnitDTO()
          .filteredRealWorkIndicatorPrecent(monthFilteredRealWorkIndicatorPrecent)
          .filteredNonWorkIndicatorPrecent(monthFilteredNonWorkIndicatorPrecent)
          .indicatorPrecent(monthIndicatorPrecent)
          .filteredSummary(monthFilteredSummary)
          .summary(monthSummary)
          .formattedExpectedWorkTime(formattedExpectedWorkTimeInMonth)
          .formattedNonWorkTime(formattedNonWorkTimeInMonth)
          .filteredPercent(monthFilteredPercent);

      return new SummaryDTO()
          .day(day)
          .hoursPerDayFormatted(hoursPerDayFormatted)
          .daySumIndustryFormatted(daySumIndustryFormatted)
          .dailyPercent(dailyPercent)
          .week(week)
          .month(month);
    }

    private String format(final Long seconds) {
      if (seconds == null) {
        return "";
      }
      return durationFormatter.exactDuration(seconds);
    }

    private Date getWeekStart(final Date date) {
      Calendar c = createNewCalendarWithWeekStart();
      c.setTime(date);

      int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
      int firstDayOfWeek = c.getFirstDayOfWeek();
      if (dayOfWeek < firstDayOfWeek) {
        dayOfWeek += DateTimeConverterUtil.DAYS_PER_WEEK;
      }
      int days = dayOfWeek - firstDayOfWeek;
      c.add(Calendar.DAY_OF_MONTH, -days);
      Date firstDate = c.getTime();
      return firstDate;
    }

    private boolean isIssuePatternsNotEmpty() {
      return (issuePatterns != null) && !issuePatterns.isEmpty();
    }
  }

  private double dailyPercent;

  private SummaryUnitDTO day;

  private String daySumIndustryFormatted;

  private String hoursPerDayFormatted;

  private SummaryUnitDTO month;

  private SummaryUnitDTO week;

  private SummaryDTO() {
  }

  private SummaryDTO dailyPercent(final double dailyPercent) {
    this.dailyPercent = dailyPercent;
    return this;
  }

  private SummaryDTO day(final SummaryUnitDTO day) {
    this.day = day;
    return this;
  }

  private SummaryDTO daySumIndustryFormatted(final String daySumIndustryFormatted) {
    this.daySumIndustryFormatted = daySumIndustryFormatted;
    return this;
  }

  public double getDailyPercent() {
    return dailyPercent;
  }

  public SummaryUnitDTO getDay() {
    return day;
  }

  public String getDaySumIndustryFormatted() {
    return daySumIndustryFormatted;
  }

  public String getHoursPerDayFormatted() {
    return hoursPerDayFormatted;
  }

  public SummaryUnitDTO getMonth() {
    return month;
  }

  public SummaryUnitDTO getWeek() {
    return week;
  }

  private SummaryDTO hoursPerDayFormatted(final String hoursPerDayFormatted) {
    this.hoursPerDayFormatted = hoursPerDayFormatted;
    return this;
  }

  private SummaryDTO month(final SummaryUnitDTO month) {
    this.month = month;
    return this;
  }

  private SummaryDTO week(final SummaryUnitDTO week) {
    this.week = week;
    return this;
  }

}
