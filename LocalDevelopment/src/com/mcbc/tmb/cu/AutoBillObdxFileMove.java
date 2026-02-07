package com.mcbc.tmb.cu;

import com.temenos.api.TField;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ebcommonparamtmb.EbCommonParamTmbRecord;
import com.temenos.t24.api.records.ebcommonparamtmb.ParamNameClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.tafj.api.client.impl.T24Context;
import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AutoBillObdxFileMove extends ServiceLifecycle {
  private static String BASE_PATH = "";
  
  private static String LOAD_DIR = "";
  
  private static String ERROR_DIR = "";
  
  private static String DONE_DIR = "";
  
  public List<String> getIds(ServiceData serviceData, List<String> controlList) {
    List<String> fileNames = new ArrayList<>();
    try {
      DataAccess dataAccess = new DataAccess((T24Context)this);
      EbCommonParamTmbRecord paramRecord = new EbCommonParamTmbRecord(
          dataAccess.getRecord("EB.COMMON.PARAM.TMB", "AUTOMATIC.BILLING.PATH"));
      for (ParamNameClass param : paramRecord.getParamName()) {
        if (param.getParamName().getValue().contains("AUTO.BILL.PATH")) {
          BASE_PATH = ((TField)param.getParamValue().get(0)).getValue();
          LOAD_DIR = String.valueOf(BASE_PATH) + "load";
//          System.out.println("Load Dir is " + LOAD_DIR);
          ERROR_DIR = String.valueOf(BASE_PATH) + "error";
          DONE_DIR = String.valueOf(BASE_PATH) + "done";
          break;
        } 
      } 
//      System.out.println("Load Dir is------------------------------------------------------------ " + LOAD_DIR);
//      System.out.println("Base Dir is------------------------------------------------------------ " + BASE_PATH);
      File loadFolder = new File(LOAD_DIR);
      if (!loadFolder.exists() || !loadFolder.isDirectory()) {
//        System.out.println("Load directory does not exist: " + LOAD_DIR);
        return fileNames;
      } 
      File[] files = loadFolder.listFiles();
      if (files != null) {
        byte b;
        int i;
        File[] arrayOfFile;
        for (i = (arrayOfFile = files).length, b = 0; b < i; ) {
          File file = arrayOfFile[b];
          if (file.isFile())
            fileNames.add(file.getName()); 
          b++;
        } 
      } 
    } catch (Exception e) {
//      System.err.println("Error in getIds: " + e.getMessage());
    } 
    return fileNames;
  }
  
  public void processSingleThreaded(ServiceData serviceData) {
    File loadFolder = new File(LOAD_DIR);
    if (!loadFolder.exists() || !loadFolder.isDirectory()) {
//      System.out.println("Load directory does not exist: " + LOAD_DIR);
      return;
    } 
    File[] files = loadFolder.listFiles();
    if (files == null || files.length == 0) {
//      System.out.println("No files to process in: " + LOAD_DIR);
      return;
    } 
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    byte b;
    int i;
    File[] arrayOfFile1;
    for (i = (arrayOfFile1 = files).length, b = 0; b < i; ) {
      File file = arrayOfFile1[b];
      if (file.isFile()) {
        String fileName = file.getName();
        String timestamp = LocalDateTime.now().format(formatter);
        Path sourcePath = file.toPath();
        String templateFileName = fileName;
        try {
          Path targetPath;
          String newFileName;
          DataAccess dataAccess = new DataAccess((T24Context)this);
          EbCommonParamTmbRecord fileParamRecord = new EbCommonParamTmbRecord(
              dataAccess.getRecord("EB.COMMON.PARAM.TMB", "AUTOMATIC.BILLING.FILE.NAME"));
          for (ParamNameClass param : fileParamRecord.getParamName()) {
            if (param.getParamName().getValue().contains("FILE.NAME")) {
              templateFileName = ((TField)param.getParamValue().get(0)).getValue();
              break;
            } 
          } 
          int dotIndex = templateFileName.lastIndexOf(".");
          if (dotIndex > 0) {
            String namePart = templateFileName.substring(0, dotIndex);
            String extPart = templateFileName.substring(dotIndex);
            newFileName = String.valueOf(namePart) + "_" + timestamp + extPart;
          } else {
            newFileName = String.valueOf(templateFileName) + "_" + timestamp;
          } 
          if (fileName.contains("ERROR")) {
            targetPath = Paths.get(ERROR_DIR, new String[] { newFileName });
          } else {
            targetPath = Paths.get(DONE_DIR, new String[] { newFileName });
          } 
          Files.createDirectories(targetPath.getParent(), (FileAttribute<?>[])new FileAttribute[0]);
          Files.move(sourcePath, targetPath, new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
          System.out.println("Moved file: " + fileName + " -> " + targetPath);
        } catch (Exception e) {
          System.err.println("Failed to move file " + fileName + ": " + e.getMessage());
        } 
      } 
      b++;
    } 
  }
}
