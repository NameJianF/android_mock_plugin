package com.bilibili.comicplugin.base;

import com.bilibili.comicplugin.model.TransformCallBack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

class JarUtils {
    public static HashSet<String> scanJarFile(File jarFile) {
        HashSet<String> hashSet = new HashSet<>();
        JarFile file = null;
        try {
            file = new JarFile(jarFile);
            Enumeration enumeration = file.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    hashSet.add(entryName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (file != null)
                    file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hashSet;
    }

    public static void deleteJarScan(File jarFile, List<String> removeClasses, DeleteCallBack deleteCallBack) throws IOException {
        JarFile file = new JarFile(jarFile);
        Enumeration enumeration = file.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement();
            String entryName = jarEntry.getName();
            if (entryName.endsWith(".class") && removeClasses.contains(entryName)) {
                InputStream inputStream = file.getInputStream(jarEntry);
                byte[] sourceClassBytes = IOUtils.toByteArray(inputStream);
                try {
                    if (deleteCallBack != null) {
                        deleteCallBack.delete(entryName, sourceClassBytes);
                    }
                } catch (Exception ignored) {

                }
            }

        }
        file.close();
    }

    static void deleteJarScan(File jarFile, DeleteCallBack callBack) throws IOException {
        /**
         * 读取原jar
         */
        JarFile file = new JarFile(jarFile);
        Enumeration enumeration = file.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement();
            InputStream inputStream = file.getInputStream(jarEntry);
            String entryName = jarEntry.getName();
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream);
            if (entryName.endsWith(".class")) {
                try {
                    if (callBack != null) {
                        callBack.delete(entryName, sourceClassBytes);
                    }
                } catch (Exception ignored) {

                }
            }

        }
        file.close();
    }

    public static File modifyJarFile(File jarFile, File tempDir, TransformCallBack callBack, BaseTransform transform) throws IOException {
        /** 设置输出到的jar */
        String hexName = DigestUtils.md5Hex(jarFile.getAbsolutePath()).substring(0, 8);
        File optJar = new File(tempDir, hexName + jarFile.getName());
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));
        /*
        读取原jar
         */
        JarFile file = new JarFile(jarFile);
        Enumeration<JarEntry> enumeration = file.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();
            InputStream inputStream = file.getInputStream(jarEntry);
            String entryName = jarEntry.getName();
            ZipEntry zipEntry = new ZipEntry(entryName);
            jarOutputStream.putNextEntry(zipEntry);
            byte[] modifiedClassBytes = null;
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream);
            if (entryName.endsWith(".class")) {
                modifiedClassBytes = callBack.process(ClassUtils.path2Classname(entryName), sourceClassBytes, transform);
            }
            if (modifiedClassBytes == null) {
                jarOutputStream.write(sourceClassBytes);
            } else {
                jarOutputStream.write(modifiedClassBytes);
            }
            jarOutputStream.closeEntry();

        }
        jarOutputStream.close();
        file.close();
        return optJar;
    }
}
