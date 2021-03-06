/*
 * Copyright (C) 2015 Víctor Suárez <victorjss@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.lexcrypta.web.up.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import net.lexcrypta.core.conf.CoreHelper;
import net.lexcrypta.core.storage.StorageService;

/**
 *
 * @author Víctor Suárez <victorjss@gmail.com>
 */
@WebServlet(urlPatterns = {"/upload"})
@MultipartConfig (fileSizeThreshold = 512 * 1024, maxFileSize = 5*1024*1024, maxRequestSize = 6*1024*1024)
public class UpServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {
        Part part = req.getPart("lexfile");
        if (part == null || part.getSize() == 0) {
            resp.sendRedirect("index.jsp");
            return;
        }
        InputStream content = part.getInputStream();
        StorageService service = new StorageService();
        String seed = getSeed(req);
        if (seed == null || "".equals(seed.trim()) || seed.length() < 6) {
            resp.sendRedirect("index.jsp");
            return;
        }
        byte[] key = service.encryptContent(content, part.getSubmittedFileName(), seed);
        String base64Key = Base64.getEncoder().encodeToString(key);
        req.getSession().setAttribute("base64Key", base64Key);
        String url = getDownloadUrl(base64Key);
        req.getSession().setAttribute("url", url);
        resp.sendRedirect("result.jsp");
    }

    protected String getDownloadUrl(String base64Key) {
        CoreHelper coreHelper = new CoreHelper();
        String downloadBaseUrl = coreHelper.getConfigurationValue("web.download.base.url");
        try {
            return downloadBaseUrl + "?key=" + URLEncoder.encode(base64Key, "utf-8");
        } catch (UnsupportedEncodingException e) {
            //weird error, we hardcoded the encoding
            throw new RuntimeException(e);
        }
    }

    /**
     * Get seed from input form, by default is the "seed" HTTP parameter.
     * Override this for different seed acquisition methods.
     * @param req Request object
     * @return String with the initial value for encryption process
     */
    protected String getSeed(HttpServletRequest req) {
        return req.getParameter("seed");
    }

}
