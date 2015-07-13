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
package net.lexcrypta.core.crypto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Víctor Suárez <victorjss@gmail.com>
 */
public class CryptoHelperTest {
    String plainText = "This is a encryption test!!!";
    String base64EncryptedText = "zy2FE8sd/4WK3mMBVyay0GNYFa7CwZAkKWsqDRFf7og=";
    String base64Secret = "mVMtHSqtTHF3JBaXoaA+/Q==";
    String iv = "12345678Z0000000"; //128 bits = 16 bytes
    
    public CryptoHelperTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

     @Test
     public void testEncrypt() throws Exception {
         CryptoHelper helper = new CryptoHelper();
         String ivBase64 = Base64.getEncoder().encodeToString(iv.getBytes("utf-8"));
         InputStream encrypted = helper.encrypt(new ByteArrayInputStream(plainText.getBytes()), ivBase64, base64Secret);
         assertEquals(base64EncryptedText, Base64.getEncoder().encodeToString(IOUtils.toByteArray(encrypted)));
     }

     @Test
     public void testDecrypt() throws Exception {
         CryptoHelper helper = new CryptoHelper();
         String ivBase64 = Base64.getEncoder().encodeToString(iv.getBytes("utf-8"));
         InputStream unencrypted = helper.decrypt(new ByteArrayInputStream(Base64.getDecoder().decode(base64EncryptedText)), ivBase64, base64Secret);
         assertEquals(plainText, new String(IOUtils.toByteArray(unencrypted), "utf-8"));
     }
}
