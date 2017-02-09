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
window.missing_days_report = window.missing_days_report || {}
everit.jttp.missing_days_report = everit.jttp.missing_days_report || {};

(function(jttp, jQuery) {

  jQuery(document).ready(function() {
    jttp.setCheckWorkedHours();
  });
  
  jttp.setCheckWorkedHours = function() {
    if (jQuery("#hour").is(":checked")) {
      document.getElementById("nonworking").disabled = false;
    } else {
      document.getElementById("nonworking").disabled = true;
    }
  }
  
  function timeZoneCorrection(date){
    var osTimeZoneOffset = date.getTimezoneOffset() * -60000;
    var correctMil = date.getTime() + osTimeZoneOffset;
    return correctMil;
  }
  
  jttp.beforeSubmitMissingsReport = function() {
    try{
      var dateFrom = jQuery('#dateFrom').val();
      var dateFromMil = Date.parseDate(dateFrom, everit.jttp.report_common_scripts.options.dateFormat);
  	  var offset=dateFromMil.getTimezoneOffset()*60000;
  	  var fromdateInUTCMilisec=dateFromMil.getTime()-offset;
      if(dateFrom != dateFromMil.print(everit.jttp.report_common_scripts.options.dateFormat)){
        showErrorMessage("error_message_label_df");
        return false;
      }
      jQuery('#dateFromMil').val(fromdateInUTCMilisec);
    }catch(err){
      showErrorMessage("error_message_label_df");
      return false;
    }
    try{
      var dateTo = jQuery('#dateTo').val();
      var dateToMil = Date.parseDate(dateTo, everit.jttp.report_common_scripts.options.dateFormat);
  	  var todateInUTCMilisec=dateToMil.getTime()-offset;
      if(dateTo != dateToMil.print(everit.jttp.report_common_scripts.options.dateFormat)){
        showErrorMessage("error_message_label_dt");
        return false;
      }
      jQuery('#dateToMil').val(todateInUTCMilisec);
    }catch(err){
      showErrorMessage("error_message_label_dt");
      return false;
    }
    if (jQuery("#hour").is(":checked")) {
      var hourClone = jQuery("#hour").clone();
      hourClone.attr("hidden", "hidden");
      jQuery("#reporting-form").append(hourClone);
    }
    if (jQuery("#nonworking").is(":checked")) {
      var noneworkingClone = jQuery("#nonworking").clone();
      noneworkingClone.attr("hidden", "hidden");
      jQuery("#reporting-form").append(noneworkingClone);
    }
  }

  jttp.beforeSubmitMissingsPagingReport = function() {
    if (jQuery("#hour").is(":checked")) {
      var hourClone = jQuery("#hour").clone();
      hourClone.attr("hidden", "hidden");
      jQuery("#paging-form").append(hourClone);
    }
    if (jQuery("#nonworking").is(":checked")) {
      var noneworkingClone = jQuery("#nonworking").clone();
      noneworkingClone.attr("hidden", "hidden");
      jQuery("#paging-form").append(noneworkingClone);
    }
    return true;
  }
  
  function showErrorMessage(message_key){
    jQuery('#error_message label').hide();
    var errorMessageLabel = jQuery('#'+message_key);
    errorMessageLabel.show();
    var errorMessage = jQuery('#error_message');
    errorMessage.show();
  }
})(everit.jttp.missing_days_report, jQuery);