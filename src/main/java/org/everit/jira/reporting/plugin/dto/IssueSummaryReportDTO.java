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

import java.util.Collections;
import java.util.List;

/**
 * Contains information to issue summary report table.
 */
public class IssueSummaryReportDTO {

  private List<IssueSummaryDTO> issueSummaries = Collections.emptyList();

  private Long issueSummaryCount = 0L;

  private PagingDTO paging = new PagingDTO();

  public List<IssueSummaryDTO> getIssueSummaries() {
    return issueSummaries;
  }

  public Long getIssueSummaryCount() {
    return issueSummaryCount;
  }

  public PagingDTO getPaging() {
    return paging;
  }

  public IssueSummaryReportDTO issueSummaries(final List<IssueSummaryDTO> issueSummaries) {
    this.issueSummaries = issueSummaries;
    return this;
  }

  public IssueSummaryReportDTO issueSummaryCount(final Long issueSummaryCount) {
    this.issueSummaryCount = issueSummaryCount;
    return this;
  }

  public IssueSummaryReportDTO paging(final PagingDTO paging) {
    this.paging = paging;
    return this;
  }

}
