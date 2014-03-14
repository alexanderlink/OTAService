/*
 * #%L
 * Over-the-air deployment webapp
 * %%
 * Copyright (C) 2012 SAP AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.sap.prd.mobile.ios.ota.webapp;

import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_ACTION;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_QRCODE;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REFERER;
import static com.sap.prd.mobile.ios.ota.lib.Constants.KEY_REMOVE_OUTER_FRAME;
import static com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.generateHtmlServiceUrl;
import static com.sap.prd.mobile.ios.ota.webapp.OtaPlistService.PLIST_SERVICE_SERVLET_NAME;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_OFF_COLOR;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_OFF_COLOR_DEFAULT;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_ON_COLOR;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.QR_ON_COLOR_DEFAULT;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getMatrixToImageConfig;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getParametersAndReferer;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.getRequestInfosForLog;
import static com.sap.prd.mobile.ios.ota.webapp.Utils.sendQRCode;
import static java.lang.String.format;
import static java.util.logging.Level.SEVERE;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.sap.prd.mobile.ios.ota.lib.Constants;
import com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator;
import com.sap.prd.mobile.ios.ota.lib.OtaHtmlGenerator.Parameters;
import com.sap.prd.mobile.ios.ota.lib.OtaPlistGenerator;

@SuppressWarnings("serial")
public class OtaHtmlService extends BaseServlet
{

  private static final Logger LOG = Logger.getLogger(OtaPlistService.class.getSimpleName());

  public static final String HTML_SERVICE_SERVLET_NAME = "otaHtmlService";

  public static final String HTML_TEMPLATE_PATH_KEY = "htmlTemplatePath";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    //TODO: REWORK. PlistService now uses Base64+URLEncoded parameters, and no URL Parameters but slashes!

    try {

      Map<String, String> params = getParametersAndReferer(request, response, true);

      LOG.info(format("GET request from '%s' with referer '%s', action:qrcode and parameters %s",
            request.getRemoteAddr(), params.get(KEY_REFERER), params));

      final String action = params.get(KEY_ACTION);
      if (StringUtils.equals(action, KEY_QRCODE)) {

        URL htmlServiceUrl = equalsIgnoreCase(params.get(KEY_REMOVE_OUTER_FRAME), "true") ?
              generateHtmlServiceUrl(getHtmlServiceBaseUrl(request), params) :
              new URL(params.get(KEY_REFERER));

        LOG.fine("Sending QRCode for " + htmlServiceUrl.toString());
        sendQRCode(request, response, htmlServiceUrl.toString(), getMatrixToImageConfig(request),
              new Dimension(400, 400));

      }
      else {

        URL plistUrl = OtaPlistGenerator.generatePlistRequestUrl(getPlistServiceBaseUrl(request), params);
        URL htmlServiceQrcodeUrl = generateHtmlServiceQRCodeUrl(request, params);

        String htmlTemplatePath = getInitParameter(HTML_TEMPLATE_PATH_KEY);
        final boolean DEBUG = equalsIgnoreCase(getInitParameter(Constants.KEY_DEBUG), "true");

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        OtaHtmlGenerator generator = OtaHtmlGenerator.getInstance(htmlTemplatePath, DEBUG);
        LOG.finer("Using HTML Template: " + generator.getTemplateName() + " (configured: " + htmlTemplatePath + ")");
        generator.generate(writer, new Parameters(plistUrl, htmlServiceQrcodeUrl, params, getInitParameters()));
        writer.flush();
      }

    }
    catch (Exception e) {
      LOG.log(SEVERE, format("Exception while processing GET request from '%s' (%s)",
            request.getRemoteAddr(), getRequestInfosForLog(request)), e);
    }
  }

  private URL getHtmlServiceBaseUrl(HttpServletRequest request) 
        throws MalformedURLException
  {
    return getServiceBaseUrl(request, HTML_SERVICE_SERVLET_NAME);
  }
  
  private URL getPlistServiceBaseUrl(HttpServletRequest request) throws MalformedURLException
  {
    return getServiceBaseUrl(request, PLIST_SERVICE_SERVLET_NAME);
  }

  private URL generateHtmlServiceQRCodeUrl(HttpServletRequest request, Map<String, String> params)
        throws MalformedURLException
  {
    URL htmlServiceUrl = generateHtmlServiceUrl(getHtmlServiceBaseUrl(request), params);
    return new URL(htmlServiceUrl.toExternalForm() + "&" +
          KEY_ACTION + "=" + KEY_QRCODE + "&" +
          QR_ON_COLOR + "=" + QR_ON_COLOR_DEFAULT + "&" +
          QR_OFF_COLOR + "=" + QR_OFF_COLOR_DEFAULT);
  }

}
