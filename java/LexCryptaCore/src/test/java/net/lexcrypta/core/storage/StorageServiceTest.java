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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Base64;
import net.lexcrypta.core.crypto.CryptoHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Víctor Suárez <victorjss@gmail.com>
 */
public class StorageServiceTest {

    public StorageServiceTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRightPad() {
        StorageService storage = new StorageService();
        
        assertEquals("1234567890000000", storage.rightPad("123456789", '0'));
        assertEquals("123456789zzzzzzz", storage.rightPad("123456789", 'z'));
        assertEquals("123456789aaaaaaa", storage.rightPad("123456789aaaaaaabbbbbbb", 'z'));
    }
    
    @Test
    public void testEncryptString() throws Exception {
        String plainText = "This is a encryption test!!!";
        byte[] encryptedText = Base64.getDecoder().decode("zy2FE8sd/4WK3mMBVyay0GNYFa7CwZAkKWsqDRFf7og=");
        byte[] key = Base64.getDecoder().decode("mVMtHSqtTHF3JBaXoaA+/Q==");
        byte[] iv = "12345678Z0000000".getBytes("utf-8"); //128 bits = 16 bytes
        
        assertArrayEquals(encryptedText, new StorageService().encryptString(plainText, iv, key));

    }
    
    @Test
    public void testDecryptString() throws Exception {
        String plainText = "This is a encryption test!!!";
        byte[] encryptedText = Base64.getDecoder().decode("zy2FE8sd/4WK3mMBVyay0GNYFa7CwZAkKWsqDRFf7og=");
        byte[] key = Base64.getDecoder().decode("mVMtHSqtTHF3JBaXoaA+/Q==");
        byte[] iv = "12345678Z0000000".getBytes("utf-8"); //128 bits = 16 bytes
        
        assertEquals(plainText, new StorageService().decryptString(encryptedText, iv, key));

    }
    
    @Test
    public void testGetIv() throws Exception {
        String seed1 = "12345678";
        String seed2 = "12345678901234567890";
        
        StorageService service = new StorageService();
        
        assertArrayEquals("12345678!!!!!!!!".getBytes("utf-8"), service.getIv(seed1));
        assertArrayEquals("1234567890123456".getBytes("utf-8"), service.getIv(seed2));

        try {
            service.getIv(null);
            fail("NullPointerExceptin expected");
        } catch (NullPointerException expected) {
            assertTrue(true);
        }
        try {
            service.getIv("");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
        try {
            service.getIv("123");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }
    
    @Test
    public void testDoEncryptContent() throws Exception {
        ByteArrayInputStream noTestedBais = new ByteArrayInputStream(new byte[512]);
        File noTestedTempFile = File.createTempFile("dummy", ".aes");

        StorageService service = new StorageService();
        
        String seed = "123456";
        byte[] key = new CryptoHelper().getNewKey();
        byte[] iv =  service.getIv(seed);
        byte[] id = service.encryptString(seed, iv, key);
        byte[] encryptedPath = service.encryptString(noTestedTempFile.getPath(), iv, key);
        
        EncryptedData ed = service.doEncryptContent(noTestedBais, noTestedTempFile,
                seed, key);
        
        assertArrayEquals(key, ed.getKey());
        assertArrayEquals(id, ed.getId());
        assertArrayEquals(encryptedPath, ed.getEncryptedPath());
        
        
    }
    
    @Test
    public void testGetPathFromDatabase() throws Exception {
        byte[] iv = "12345678Z0000000".getBytes("utf-8"); //128 bits = 16 bytes
        byte[] key = Base64.getDecoder().decode("mVMtHSqtTHF3JBaXoaA+/Q==");
        
        StorageService service = new StorageService();
        
        String seed = "abcdefg";
        byte[] encryptedSeed = service.encryptString(seed, iv, key);
        String b64EncryptedSeed = Base64.getEncoder().encodeToString(encryptedSeed);
        
        String path = "/lexcrypta/java/iutest/getPathFromDatabase";
        byte[] encryptedPath = service.encryptString(path, iv, key);
        String b64EncryptedPath = Base64.getEncoder().encodeToString(encryptedPath);
        
        Class.forName("org.hsqldb.jdbcDriver");
        Connection c = DriverManager.getConnection("jdbc:hsqldb:mem:testdb");
        PreparedStatement ps = c.prepareStatement("CREATE TABLE lexcrypta (id VARCHAR(512), filepath VARCHAR(2048))");
        ps.executeUpdate();
        ps.close();
        ps = c.prepareStatement("INSERT INTO lexcrypta (id, filepath) VALUES (?, ?)");
        ps.setString(1, b64EncryptedSeed); //id
        ps.setString(2, b64EncryptedPath); //path
        ps.executeUpdate();
        ps.close();
        
        service.coreHelper.setTestConnection(c);

        assertEquals(path, service.getPathFromDatabase(encryptedSeed, iv, key));
    }
}
