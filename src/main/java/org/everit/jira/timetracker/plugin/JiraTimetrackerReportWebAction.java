package org.everit.jira.timetracker.plugin;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.everit.jira.timetracker.plugin.dto.EveritWorklog;
import org.ofbiz.core.entity.GenericEntityException;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class JiraTimetrackerReportWebAction extends JiraWebActionSupport {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
            .getLogger(JiraTimetrackerReportWebAction.class);
    /**
     * The {@link JiraTimetrackerPlugin}.
     */
    private final JiraTimetrackerPlugin jiraTimetrackerPlugin;
    /**
     * The date.
     */
    private Date dateFrom = null;
    /**
     * The formated date.
     */
    private String dateFromFormated = "";
    /**
     * The date.
     */
    private Date dateTo = null;
    /**
     * The formated date.
     */
    private String dateToFormated = "";
    /**
     * The message.
     */
    private String message = "";

    private String contextPath;

    /**
     * The first day of the week
     */
    private int fdow;

    private Map<String, List<EveritWorklog>> allWorklogsMap;

    private Map<String, String> daysRealWorklogSummary;

    private List<User> allUsers;

    private String currentUser = "";

    private String avatarURL = "";

    /**
     * Jira Componentmanager instance.
     */
    private final ComponentManager componentManager = ComponentManager.getInstance();

    private User userPickerObject;

    /**
     * Simple constructor.
     *
     * @param jiraTimetrackerPlugin
     *            The {@link JiraTimetrackerPlugin}.
     */
    public JiraTimetrackerReportWebAction(
            final JiraTimetrackerPlugin jiraTimetrackerPlugin) {
        this.jiraTimetrackerPlugin = jiraTimetrackerPlugin;
    }

    /**
     * Set dateFrom and dateFromFormated default value.
     */
    private void dateFromDefaultInit() {
        final Calendar calendarFrom = Calendar.getInstance();
        calendarFrom.add(Calendar.WEEK_OF_MONTH, -1);
        dateFrom = calendarFrom.getTime();
        dateFromFormated = DateTimeConverterUtil.dateToString(dateFrom);
    }

    /**
     * Set dateTo and dateToFormated default value.
     */
    private void dateToDefaultInit() {
        final Calendar calendarTo = Calendar.getInstance();
        dateTo = calendarTo.getTime();
        dateToFormated = DateTimeConverterUtil.dateToString(dateTo);
    }

    @Override
    public String doDefault() throws ParseException {
        final boolean isUserLogged = JiraTimetrackerUtil.isUserLogged();
        if (!isUserLogged) {
            setReturnUrl("/secure/Dashboard.jspa");
            return getRedirect(NONE);
        }

        normalizeContextPath();
        jiraTimetrackerPlugin.loadPluginSettings();
        fdow = jiraTimetrackerPlugin.getFdow();

        if (dateFromFormated.equals("")) {
            dateFromDefaultInit();
        }
        if (dateToFormated.equals("")) {
            dateToDefaultInit();
        }

        allWorklogsMap = null;

        allUsers = new ArrayList<User>(UserUtils.getAllUsers());
        Collections.sort(allUsers);

        final JiraAuthenticationContext authenticationContext = componentManager.getJiraAuthenticationContext();
        currentUser = authenticationContext.getLoggedInUser().getName();
        setUserPickerObjectBasedOnSelectedUser();

        return INPUT;
    }

    @Override
    public String doExecute() throws ParseException {
        final boolean isUserLogged = JiraTimetrackerUtil.isUserLogged();
        if (!isUserLogged) {
            setReturnUrl("/secure/Dashboard.jspa");
            return getRedirect(NONE);
        }

        normalizeContextPath();
        jiraTimetrackerPlugin.loadPluginSettings();
        fdow = jiraTimetrackerPlugin.getFdow();

        allUsers = new ArrayList<User>(UserUtils.getAllUsers());
        Collections.sort(allUsers);

        currentUser = request.getParameterValues("userPicker")[0];

        if (dateFromFormated.equals("")) {
            dateFromDefaultInit();
        }
        if (dateToFormated.equals("")) {
            dateToDefaultInit();
        }
        if ((currentUser == null) || currentUser.equals("")) {
            final JiraAuthenticationContext authenticationContext = componentManager.getJiraAuthenticationContext();
            currentUser = authenticationContext.getLoggedInUser().getName();
        }
        setUserPickerObjectBasedOnSelectedUser();

        final String dateFrom = request.getParameterValues("dateFrom")[0];
        if ((dateFrom != null) && !dateFrom.equals("")) {
            dateFromFormated = dateFrom;
        }
        else {
            message = "plugin.invalid_startTime";
            return SUCCESS;
        }
        final Calendar startDate = Calendar.getInstance();
        startDate.setTime(DateTimeConverterUtil.stringToDate(dateFrom));

        final String dateTo = request.getParameterValues("dateTo")[0];
        if ((dateTo != null) && !dateTo.equals("")) {
            dateToFormated = dateTo;
        }
        else {
            message = "plugin.invalid_endTime";
            return SUCCESS;
        }

        if (dateFrom.compareTo(dateTo) > 0) {
            message = "plugin.wrong.dates";
            return SUCCESS;
        }

        final Calendar lastDate = (Calendar) startDate.clone();
        lastDate.setTime(DateTimeConverterUtil.stringToDate(dateTo));

        final List<EveritWorklog> worklogs = new ArrayList<EveritWorklog>();
        try {
            worklogs.addAll(jiraTimetrackerPlugin.getWorklogs(currentUser, startDate.getTime(), lastDate.getTime()));
        } catch (final DataAccessException e) {
            LOGGER.error("Error when trying to get worklogs.", e);
            return ERROR;
        } catch (final SQLException e) {
            LOGGER.error("Error when trying to get worklogs.", e);
            return ERROR;
        }
        allWorklogsMap = new LinkedHashMap<String, List<EveritWorklog>>();
        daysRealWorklogSummary = new LinkedHashMap<String, String>();
        // TODO go over the dates (from - to) and put in the map the empty days too!
        // TODO add weekends and holidays
        for (final EveritWorklog worklog : worklogs) {
            final String dateOfTheWorklog = worklog.getStartDate();
            final List<EveritWorklog> newValue = new ArrayList<EveritWorklog>();
            final List<EveritWorklog> oldValue = allWorklogsMap.get(dateOfTheWorklog);
            if (oldValue == null) {
                newValue.add(worklog);
                allWorklogsMap.put(dateOfTheWorklog, newValue);
            } else {
                oldValue.add(worklog);
                allWorklogsMap.put(dateOfTheWorklog, oldValue);
            }
            try {
                // TODO summary not just for days
                makeSummary(dateOfTheWorklog);
            } catch (final GenericEntityException e) {
                LOGGER.error("Error when trying to get day worklog summary.", e);
                return ERROR;
            }
        }
        return SUCCESS;
    }

    public List<User> getAllUsers() {
        return allUsers;
    }

    public Map<String, List<EveritWorklog>> getAllWorklogsMap() {
        return allWorklogsMap;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getCurrentUserEmail() {
        return currentUser;
    }

    public String getDateFromFormated() {
        return dateFromFormated;
    }

    public String getDateToFormated() {
        return dateToFormated;
    }

    public Map<String, String> getDaysRealWorklogSummary() {
        return daysRealWorklogSummary;
    }

    public int getFdow() {
        return fdow;
    }

    public String getMessage() {
        return message;
    }

    public User getUserPickerObject() {
        return userPickerObject;
    }

    private void makeSummary(final String dateOfTheWorklog) throws ParseException, GenericEntityException {
        String dateRealworklogSummary;
        final Date date = DateTimeConverterUtil.stringToDate(dateOfTheWorklog);
        final Calendar startCalendar = DateTimeConverterUtil.setDateToDayStart(date);
        // Calendar originalStartcalendar = (Calendar) startCalendar.clone();
        final Date start = startCalendar.getTime();

        final Calendar endCalendar = (Calendar) startCalendar.clone();
        endCalendar.add(Calendar.DAY_OF_MONTH, 1);

        // Calendar originalEndCalendar = (Calendar) endCalendar.clone();
        final Date end = endCalendar.getTime();

        dateRealworklogSummary = jiraTimetrackerPlugin.summary(currentUser, start, end, null);
        // dateRealworklogSummary = jiraTimetrackerPlugin.summary(currentUser,
        // DateTimeConverterUtil.stringToDate(dateOfTheWorklog),
        // DateTimeConverterUtil.stringToDate(dateOfTheWorklog), null);
        daysRealWorklogSummary.put(dateOfTheWorklog, dateRealworklogSummary);
    }

    private void normalizeContextPath() {
        final String path = request.getContextPath();
        if ((path.length() > 0) && path.substring(path.length() - 1).equals("/")) {
            contextPath = path.substring(0, path.length() - 1);
        } else {
            contextPath = path;
        }
    }

    public void setAllUsers(final List<User> allUsers) {
        this.allUsers = allUsers;
    }

    public void setAllWorklogsMap(final Map<String, List<EveritWorklog>> allWorklogsMap) {
        this.allWorklogsMap = allWorklogsMap;
    }

    public void setAvatarURL(final String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public void setCurrentUser(final String currentUserEmail) {
        currentUser = currentUserEmail;
    }

    public void setDateFromFormated(final String dateFromFormated) {
        this.dateFromFormated = dateFromFormated;
    }

    public void setDateToFormated(final String dateToFormated) {
        this.dateToFormated = dateToFormated;
    }

    public void setDaysRealWorklogSummary(final Map<String, String> daysRealWorklogSummary) {
        this.daysRealWorklogSummary = daysRealWorklogSummary;
    }

    public void setFdow(final int fdow) {
        this.fdow = fdow;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setUserPickerObject(final User userPickerObject) {
        this.userPickerObject = userPickerObject;
    }

    private void setUserPickerObjectBasedOnSelectedUser() {
        if ((currentUser != null) && !currentUser.equals("")) {
            userPickerObject = componentManager.getUserUtil().getUserObject(currentUser);
            final AvatarService avatarService = ComponentManager
                    .getComponentInstanceOfType(AvatarService.class);
            setAvatarURL(avatarService.getAvatarURL(userPickerObject, currentUser, Avatar.Size.SMALL).toString());
        } else {
            userPickerObject = null;
        }
    }
}
