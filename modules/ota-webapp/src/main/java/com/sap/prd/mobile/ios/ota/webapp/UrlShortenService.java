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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("serial")
public class UrlShortenService extends HttpServlet
{

  public static final String URL_SERVICE_SERVLET_NAME = "urlShortenService";

  private static HashMap<String, String> map = new HashMap<String, String>();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    String longUrl = map.get(request.getRequestURL().toString().toLowerCase());
    if (StringUtils.isEmpty(longUrl)) {
      response.sendError(413, "No url found for " + request.getRequestURL());
    }
    else {
      response.sendRedirect(longUrl);
    }
  }

  public static String shorten(HttpServletRequest request, String longUrl) throws MalformedURLException
  {
    String shortUrl = generateShortUrl(request, longUrl);
    map.put(shortUrl.toLowerCase(), longUrl);
    return shortUrl;
  }

  private static String generateShortUrl(HttpServletRequest request, String longUrl) throws MalformedURLException
  {
    URL urlServiceBaseUrl = getUrlServiceBaseUrl(request);
    String uid = generateUid();
    return (urlServiceBaseUrl.toExternalForm() + "/" + uid);
  }

  private static long id = 0;

  private static synchronized String generateUid()
  {
    return (++id) + "";
  }

  private static URL getUrlServiceBaseUrl(HttpServletRequest request) throws MalformedURLException
  {
    return Utils.getServiceBaseUrl(request, URL_SERVICE_SERVLET_NAME);
  }

}
