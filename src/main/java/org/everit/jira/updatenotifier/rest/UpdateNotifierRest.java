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
package org.everit.jira.updatenotifier.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.everit.jira.settings.TimeTrackerSettingsHelper;
import org.everit.jira.updatenotifier.UpdateNotifier;

/**
 * Rest for the Update notifier.
 */
@Path("/update-notifier")
public class UpdateNotifierRest {

  private TimeTrackerSettingsHelper settingsHelper;

  public UpdateNotifierRest(final TimeTrackerSettingsHelper settingsHelper) {
    this.settingsHelper = settingsHelper;
  }

  /**
   * Save the current version in the user properties.
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("/cancel-update")
  public Response saveCancel() {
    UpdateNotifier updateNotifierSettingsHelper = new UpdateNotifier(settingsHelper);
    updateNotifierSettingsHelper.putDisableNotifierForVersion();
    return Response.ok().build();
  }
}
