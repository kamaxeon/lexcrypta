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
package net.lexcrypta.core.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import net.lexcrypta.core.crypto.CryptoHelper;
import net.lexcrypta.core.conf.CoreHelper;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Víctor Suárez <victorjss@gmail.com>
 */
public class StorageService {
    CryptoHelper cryptoHelper = new CryptoHelper();
    CoreHelper coreHelper = new CoreHelper();

    static volatile Properties coreProps = null;
    
    /**
     * Right pad 's' with the 'c' character to obtain a valid AES key. 
     * The number of padding characters depends on s.length(), but the resulting 
     * string will always be of the same size as the AES key. If 's' is bigger 
     * than AES key valid length, then 's' will be truncated.
     * @param s String to be padded
     * @param c Character for right padding
     * @return 's' string right padded with 'c' characters, or truncated to AES
     * key size.
     */
    public String rightPad(String s, char c) {
        StringBuilder sb = new StringBuilder(s);
        final int keyLength = cryptoHelper.getKeyLength() / 8;
        final int niters = keyLength - s.length();
        for (int i = 0; i < niters; i++) {
            sb.append(c);
        }
        return sb.substring(0, keyLength);
    }
    
    /**
     * Encrypt content, using AES with a random key and a Initialization Vector
     * generated from 'seed' parameter.
     * This method returns a EncryptedData structure with all the necessary 
     * information to allow storage in database. Encryted data is 
     * stored in file system without defined file name.
     * Returned data must be treated in this way:
     * <ul><li>key must be stored NOWHERE, only must be in the shared URL</li>
     * <li>id must be stored in database along encrypted path</li>
     * <li>encryptedPath must be stored in database along id</li></ul>
     * @param content conted to be encrypted
     * @param seed value used to genererate the AES Initialization Vector
     * @return struct with reference info of encryptation result
     */
    public EncryptedData encryptContent(InputStream content, String seed) {
        try {
            byte[] key = cryptoHelper.getNewKey();
            return doEncryptContent(content, seed, key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method was created/refactored for facilitating development
     * of integration tests. 
     * This method builds a EncryptedData structure from provided parameters.
     * @param content conted to be encrypted
     * @param seed value used to genererate the AES Initialization Vector
     * @param key AES key used for encryption
     * @return struct with reference info of encryptation result
     * @throws IOException
     * @throws FileNotFoundException 
     */
    protected EncryptedData doEncryptContent(InputStream content, 
            String seed,
            byte[] key)
            throws IOException, FileNotFoundException {
        
        byte[] iv = getIv(seed);
        InputStream encryptedStream = cryptoHelper.encrypt(content, iv, key);
        
        String targetDirPath = getTargetDirPath();
        File targetFile = getTargetFile(targetDirPath);

        FileOutputStream fos = new FileOutputStream(targetFile);
        IOUtils.copyLarge(encryptedStream, fos, new byte[512]);
        
        byte[] id = encryptString(seed, iv, key);
        byte[] encryptedPath = encryptString(targetFile.getPath(), iv, key);
        
        EncryptedData ed = new EncryptedData();
        ed.setKey(key);
        ed.setId(id);
        ed.setEncryptedPath(encryptedPath);
        
        return ed;
    }

    /**
     * Decrypts content from provided seed and key.
     * This method search the path of the file with the encrypted data by 
     * executing a SQL similar to "select path from datatable where id=?"
     * where ID is the seed encrypted with the provided key and an initialization 
     * vector based on seed (built with #getIv(String) method).
     * Path recovered is encrypted using the same key an IV used for ID.
     * @param seed value used to genererate the AES Initialization Vector
     * @param key AES key used for encryption
     * @return an InputStream with decrypted content or null if the file is not
     * present (expired?)
     */
    public InputStream decryptContent(String seed, byte[] key) {
        try {
            byte[] iv = getIv(seed);
            byte[] id = encryptString(seed, iv, key);
            
            return getContentFromFileSystem(id, iv, key);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected InputStream getContentFromFileSystem(byte[] id,
            byte[] iv,
            byte[] key) {
        
        String path = getPathFromDatabase(id, iv, key);

        if (path != null) {
            try {
                FileInputStream fis = new FileInputStream(path);

                return cryptoHelper.decrypt(fis, iv, key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    protected String getPathFromDatabase(byte[] id,
            byte[] iv,
            byte[] key)
            throws RuntimeException {
        String idBase64 = Base64.getEncoder().encodeToString(id);
        try (
                Connection conn = coreHelper.getConnection(); 
                PreparedStatement ps = conn.prepareStatement(coreHelper.getSql(conn.getMetaData(), "select-path"));) {
            ps.setString(1, idBase64);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
            
                String encryptedPathBase64 = rs.getString(1);
                return decryptDatabasePath(encryptedPathBase64, iv, key);

            }
            
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    protected String decryptDatabasePath(String encryptedPathBase64,
            byte[] iv,
            byte[] key)
            throws IOException {
        byte[] encryptedPath = Base64.getDecoder().decode(encryptedPathBase64);
        String path = decryptString(encryptedPath, iv, key);
        return path;
    }

    protected File getTargetFile(String targetDirPath)
            throws IOException {
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        File aesFile = File.createTempFile("lex", ".aes", targetDir);
        return aesFile;
    }
    
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    protected String getTargetDirPath() {
        String basePath = coreHelper.getConfigurationValue("storage.basePath");
        String targetDirPath = basePath + File.separator + sdf.format(new Date());
        return targetDirPath;
    }

    protected byte[] encryptString(String s,
            byte[] iv,
            byte[] key)
            throws IOException, UnsupportedEncodingException {
        InputStream encryptedStream = cryptoHelper.encrypt(new ByteArrayInputStream(s.getBytes("utf-8")), iv, key);
        return IOUtils.toByteArray(encryptedStream);
    }

    protected String decryptString(byte[] encryptedString,
            byte[] iv,
            byte[] key)
            throws IOException, UnsupportedEncodingException {
        InputStream encryptedStream = cryptoHelper.decrypt(new ByteArrayInputStream(encryptedString), iv, key);
        byte[] b = IOUtils.toByteArray(encryptedStream);
        return new String(b, "utf-8");
    }

    /**
     * Generate the Initialization Vector from seed bytes
     * @param seed the seed
     * @return correct padded IV for AES use
     */
    protected byte[] getIv(String seed) {
        if (seed.length() < 6) {
            throw new IllegalArgumentException("seed too short");
        }
        try {
            return rightPad(seed, '!').getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            //weird, hardcoded UTF-8
            throw new RuntimeException(e);
        }
    }
}
