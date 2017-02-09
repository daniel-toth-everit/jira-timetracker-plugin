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
package org.everit.jira.reporting.plugin.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.everit.jira.core.util.TimetrackerUtil;

/**
 * Representation of user to picker.
 */
@XmlRootElement
public class PickerUserDTO {

  /**
   * Alias names to projections.
   */
  public static final class AliasNames {

    public static final String ACTIVE = "active";

    public static final String AVATAR_OWNER = "avatarOwner";

    public static final String DISPLAY_NAME = "displayName";

    public static final String USER_NAME = "userName";

    private AliasNames() {
    }
  }

  public static final String CURRENT_USER_DISPLAY_NAME = "jtrp.picker.current.user.value";

  public static final String CURRENT_USER_NAME = "currentUser";

  public static final String NONE_DISPLAY_NAME = "jtrp.picker.value.none";

  public static final String NONE_USER_NAME = "none";

  public static final String UNASSIGNED_DISPLAY_NAME = "jtrp.picker.unassigned.value";

  public static final String UNASSIGNED_USER_NAME = "empty";

  /**
   * Create current user.
   */
  public static PickerUserDTO createCurrentUser() {
    PickerUserDTO currentUser = new PickerUserDTO();
    String loggedUserName = TimetrackerUtil.getLoggedUserName();
    currentUser.setDisplayName(TimetrackerUtil.getI18nText(CURRENT_USER_DISPLAY_NAME));
    currentUser.setUserName(CURRENT_USER_NAME);
    currentUser.setAvatarOwner(loggedUserName);
    currentUser.setActive(1);
    return currentUser;
  }

  /**
   * Create None 'user'.
   */
  public static PickerUserDTO createNoneUser() {
    PickerUserDTO unassigned = new PickerUserDTO();
    unassigned.setDisplayName(TimetrackerUtil.getI18nText(NONE_DISPLAY_NAME));
    unassigned.setUserName(NONE_USER_NAME);
    unassigned.setAvatarOwner(NONE_USER_NAME);
    unassigned.setActive(1);
    return unassigned;
  }

  /**
   * Create unassigned 'user'.
   */
  public static PickerUserDTO createUnassignedUser() {
    PickerUserDTO unassigned = new PickerUserDTO();
    unassigned.setDisplayName(TimetrackerUtil.getI18nText(UNASSIGNED_DISPLAY_NAME));
    unassigned.setUserName(UNASSIGNED_USER_NAME);
    unassigned.setAvatarOwner(UNASSIGNED_USER_NAME);
    unassigned.setActive(1);
    return unassigned;
  }

  @XmlElement
  private int active;

  @XmlElement
  private String avatarOwner;

  @XmlElement
  private String displayName;

  @XmlElement
  private String userName;

  public int getActive() {
    return active;
  }

  public String getAvatarOwner() {
    return avatarOwner;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getUserName() {
    return userName;
  }

  public void setActive(final int active) {
    this.active = active;
  }

  public void setAvatarOwner(final String avatarOwner) {
    this.avatarOwner = avatarOwner;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  public void setUserName(final String userName) {
    this.userName = userName;
  }

}
