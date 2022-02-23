/*
 * Copyright Hyundai AutoEver.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Hyundai AutoEver. ("Confidential Information").
 */
package com.xconnect.eai.batch.databaseStorageMgr; 

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;

import com.xconnect.eai.server.common.util.CommonUtils;


public class FTPDataToJDBC {
	private boolean verbose	=	true;
	private static final Logger LOG = LoggerFactory.getLogger(FTPDataToJDBC.class);
	int ibatchSize	=	1000;
	final String CONSTANT_CHK_DIR	=	"CHK";		
	final String CONSTANT_FAIL_DIR	=	"FAIL";		
	final String CONSTANT_EXTEND_CHK	=	".checkpoint";		
	
	
	
	public String getFieldString() {
		return fieldString;
	}

	public void setFieldString(String fieldString) {
		this.fieldString = fieldString;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	String fieldString 	= "at_id,remote,unit_no,point_no,type,point_name,date_occurred,time_occurred,date_recorded,time_recorded,category,message,state_id,last_name,first_name,workgroup,card_no,udf1,udf2,udf3,udf4,udf5,udf6,udf7,last_updated,pt_id,buss,at_type,date_occurred_server,time_occurred_server,fln_no,device_no,card_facility,card_tech,new_area,old_area,db_type,db_id,bHasAlarmColors,lDisplayColor,lBackgroundColor,utc_offset,opg_id,emp_id";
	String tableName 	= "dbo.ZHHRSIEMENSCARD"; 	

	private DataSource dataSrc;
	private DataSourceTransactionManager dataSourceTransactionManager;
	private DefaultTransactionDefinition paramTransactionDefinition = new DefaultTransactionDefinition();
	private TransactionStatus status;
	private NamedParameterJdbcTemplate npjt;
	
	public void setDataSource(DataSource dataSrc) {
		this.dataSrc	=	dataSrc;
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSrc);
		dataSourceTransactionManager = new DataSourceTransactionManager();
		dataSourceTransactionManager.setDataSource(dataSrc);
		npjt = jdbcTemplate;
	}
	
	public static List parseCsv(String contents, String[] column, boolean isFirstHeader) throws Exception {
		try {
			final String token	=	"\t";
			String[] lines = contents.split("\\r?\\n");
			String[] columns = null;
			if(column != null && column.length > 0) columns = column;
			if(isFirstHeader && columns == null) {
				String header = lines[0];
				columns = header.trim().split(token);
			}
			
			List<HashMap<String, Object>> contentsMapList = new ArrayList<HashMap<String, Object>>();
			for(int i=0; i < lines.length; i++) {
				
		

				
				if(isFirstHeader && i == 0) continue;
				HashMap<String, Object> contentMap = new HashMap<>();
				String[] line = lines[i].trim().split(token);
				if(line.length != columns.length) {
					LOG.debug("[FTPDataToJDBC parseCsv]--Field counts is not match ["+columns.length+"] ["+line.length+"] ("+lines[i]+")");
					throw new Exception("Field counts is not match ["+columns.length+"] ["+line.length+"] ("+lines[i]+")");
				}
				for(int l=0; l < columns.length; l++) {
					contentMap.put(columns[l], line[l]);
				}
				contentsMapList.add(contentMap);
			}
			
			return contentsMapList;
		} catch (Exception e) {
			e.printStackTrace(System.out);
			throw e;
		}
	}	
	
	public static void printEncoding(String data) {
		String msgName = data; 
		String [] charSet = {"utf-8","euc-kr","ksc5601","iso-8859-1","x-windows-949"}; 
		
		try {
			System.out.println( "***************************************************************************************");
			
			
			System.out.println("UTF-8 -> EUC-KR        : " + new String(msgName.getBytes("UTF-8"), "EUC-KR"));
			System.out.println("UTF-8 -> KSC5601       : " + new String(msgName.getBytes("UTF-8"), "KSC5601"));
			System.out.println("UTF-8 -> X-WINDOWS-949 : " + new String(msgName.getBytes("UTF-8"), "X-WINDOWS-949"));
			System.out.println("UTF-8 -> ISO-8859-1    : " + new String(msgName.getBytes("UTF-8"), "ISO-8859-1"));
			System.out.println("UTF-8 -> MS949         : " + new String(msgName.getBytes("UTF-8"), "MS949"));
			 
			System.out.println("ISO-8859-1 -> EUC-KR        : " + new String(msgName.getBytes("ISO-8859-1"), "EUC-KR"));
			System.out.println("ISO-8859-1 -> KSC5601       : " + new String(msgName.getBytes("ISO-8859-1"), "KSC5601"));
			System.out.println("ISO-8859-1 -> X-WINDOWS-949 : " + new String(msgName.getBytes("ISO-8859-1"), "X-WINDOWS-949"));
			System.out.println("ISO-8859-1 -> UTF-8         : " + new String(msgName.getBytes("ISO-8859-1"), "UTF-8"));
			System.out.println("ISO-8859-1 -> MS949         : " + new String(msgName.getBytes("ISO-8859-1"), "MS949"));
			 
			System.out.println("EUC-KR -> UTF-8         : " + new String(msgName.getBytes("EUC-KR"), "UTF-8"));
			System.out.println("EUC-KR -> KSC5601       : " + new String(msgName.getBytes("EUC-KR"), "KSC5601"));
			System.out.println("EUC-KR -> X-WINDOWS-949 : " + new String(msgName.getBytes("EUC-KR"), "X-WINDOWS-949"));
			System.out.println("EUC-KR -> ISO-8859-1    : " + new String(msgName.getBytes("EUC-KR"), "ISO-8859-1"));
			System.out.println("EUC-KR -> MS949         : " + new String(msgName.getBytes("EUC-KR"), "MS949"));
			 
			System.out.println("KSC5601 -> EUC-KR        : " + new String(msgName.getBytes("KSC5601"), "EUC-KR"));
			System.out.println("KSC5601 -> UTF-8         : " + new String(msgName.getBytes("KSC5601"), "UTF-8"));
			System.out.println("KSC5601 -> X-WINDOWS-949 : " + new String(msgName.getBytes("KSC5601"), "X-WINDOWS-949"));
			System.out.println("KSC5601 -> ISO-8859-1    : " + new String(msgName.getBytes("KSC5601"), "ISO-8859-1"));
			System.out.println("KSC5601 -> MS949         : " + new String(msgName.getBytes("KSC5601"), "MS949"));
			 
			System.out.println("X-WINDOWS-949 -> EUC-KR     : " + new String(msgName.getBytes("X-WINDOWS-949"), "EUC-KR"));
			System.out.println("X-WINDOWS-949 -> UTF-8      : " + new String(msgName.getBytes("X-WINDOWS-949"), "UTF-8"));
			System.out.println("X-WINDOWS-949 -> KSC5601    : " + new String(msgName.getBytes("X-WINDOWS-949"), "KSC5601"));
			System.out.println("X-WINDOWS-949 -> ISO-8859-1 : " + new String(msgName.getBytes("X-WINDOWS-949"), "ISO-8859-1"));
			System.out.println("X-WINDOWS-949 -> MS949      : " + new String(msgName.getBytes("X-WINDOWS-949"), "MS949"));
			                
			System.out.println("MS949 -> EUC-KR        : " + new String(msgName.getBytes("MS949"), "EUC-KR"));
			System.out.println("MS949 -> UTF-8         : " + new String(msgName.getBytes("MS949"), "UTF-8"));
			System.out.println("MS949 -> KSC5601       : " + new String(msgName.getBytes("MS949"), "KSC5601"));
			System.out.println("MS949 -> ISO-8859-1    : " + new String(msgName.getBytes("MS949"), "ISO-8859-1"));
			System.out.println("MS949 -> X-WINDOWS-949 : " + new String(msgName.getBytes("MS949"), "X-WINDOWS-949"));			
			
			System.out.println( "***************************************************************************************");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			
			e1.printStackTrace();
		}
		
			
	}

	public void process(String textData) throws Exception {
		try {
			if(verbose) LOG.debug("[FTPDataToJDBC]----------------------------------------------------start");
			
			// 0 data get
			String[] fieldStrings	=	null;
			boolean isFirstLine		=	false;

			if(fieldString==null) new Exception("No filed data.......................................");
			fieldStrings	=	fieldString!=null?fieldString.split(","):null;

			// data insert
			List<Map> dataList	= parseCsv(textData, fieldStrings, isFirstLine);	
			int[] affected = null;
			boolean insert_fail = false;
			String insert_fail_msg = "";
			
			status = dataSourceTransactionManager.getTransaction(paramTransactionDefinition);
			try {
//				if(dataSrc.getConnection() == null) {
//					throw new Exception("DBConnection_ERROR_RETRY");
//				}
				
				boolean bDeldat	= true;
				String[] columnKeyArry 	  = { "pt_id","Date_occurred","Time_occurred","Emp_id","flag"};
				String[] columnInsertArry = { "pt_id", "Point_name","Date_occurred","Time_occurred","Message","Card_no","udf4","point_no"};
	                
				String deleteQuery	=	"DELETE FROM "+tableName +" WHERE 1=1 ";
				String insertQuery	=	"INSERT INTO "+tableName +" ( "+" pt_id	,Point_name,Date_occurred,Time_occurred,Message,Card_no,Emp_id, flag "+" ) "+
										" Values ( :pt_id , :point_name, :date_occurred, :time_occurred, :message,  :card_no, :udf4, :point_no )";
				
				List<Map<String, Object>> deleteDataMapList = new ArrayList<Map<String, Object>>();
				List<Map<String, Object>> insertDataMapList = new ArrayList<Map<String, Object>>();
				
				// make delete query
				Map<String,String> mapping	=	new HashMap<String,String>();
				mapping.put("Emp_id", "udf4");
				mapping.put("flag"  , "point_no");
				String updateConditionDetail	=	"";
				for(String keyField:columnKeyArry) {
					if(!keyField.isEmpty()) {
						String dataField	=	mapping.get(keyField);
						dataField	=	dataField==null?keyField.toLowerCase():dataField;
						updateConditionDetail = updateConditionDetail + " AND "+keyField+" = :"+dataField;
					}
				}
				deleteQuery	=	deleteQuery + updateConditionDetail; 
				
				String inGate		=	"ＩＮ";
				String outGate		=	"ＯＵＴ";
				String gates		=	PropertiesUtil.get("config", "gates.name.list");
				if(gates == null) gates = "";
				String indatadup	=	PropertiesUtil.get("config", "gates.data.dup");

				if(indatadup!=null && indatadup.equals("Y")) {
					// insert 중복체크
					setFirst(dataList,dataList);
				}

				for(Map delCondition : dataList) {
					LOG.debug("+-[ (Target) for ]---------");
					
					  if(delCondition.get("pt_id") == null || delCondition.get("pt_id").equals("") || delCondition.get("pt_id").equals("pt_id")) continue; 
					  
					  
//					  if(delCondition.get("card_no") == null ||
//					  delCondition.get("card_no").equals("")) continue; if(delCondition.get("udf4")
//					  == null || delCondition.get("udf4").equals("") ||
//					  delCondition.get("udf4").equals("0")) continue;
//					  
					  String pointName = (String) delCondition.get("point_name"); 
					  pointName = pointName==null?"":pointName; if(pointName.contentEquals("point_name"))
						  continue;
//					  
					  //!pointName.contains(inGate) && !pointName.contains(outGate) &&
					  if(!gates.contains(pointName.trim())) 
					  { 
						  continue; 
					  }
					 
					Map keyFieldDataSet		=	new HashMap();
					Map insertFieldDataSet	=	new HashMap();
					for(String keyField:columnKeyArry) {
						String dataField	=	mapping.get(keyField);
						dataField	=	dataField==null?keyField.toLowerCase():dataField;
						keyFieldDataSet.put(dataField, delCondition.get(dataField));
					}
					deleteDataMapList.add(keyFieldDataSet);
					
					String skipFlag	= "";
					if(delCondition.get("skip")!=null)
						skipFlag	=	delCondition.get("skip").toString();
					
					if(!skipFlag.equals("S"))
					for(String inField:columnInsertArry) {
						insertFieldDataSet.put(inField.toLowerCase(), delCondition.get(inField.toLowerCase()));
					}					
					
					if(!skipFlag.equals("S"))
						insertDataMapList.add(insertFieldDataSet);
				}

				LOG.debug("+-[ (Target) DB Batch delete ]-----------------------------v0.1");
				LOG.debug("+-[ (Target) DB Batch Count  ]------["+deleteDataMapList.size()+"]----");
				if(deleteDataMapList.size() > 0)
				affected = npjt.batchUpdate(deleteQuery, SqlParameterSourceUtils.createBatch(deleteDataMapList.toArray(new Map[0])));
				LOG.debug("+-[ (Target) DB Batch delete ]---------------------------------");
				
				LOG.debug("+-[ (Target) DB Batch Insert ]---------------------------------");
				LOG.debug("+-[ (Target) DB Batch Count  ]------["+insertDataMapList.size()+"]----");
				if(insertDataMapList.size() > 0)				
				LOG.debug("+-[ (Target) DB Batch insertDataMapList ]--------[" + insertDataMapList + "]");
				affected = npjt.batchUpdate(insertQuery, SqlParameterSourceUtils.createBatch(insertDataMapList.toArray(new Map[0])));
				LOG.debug("+-[ (Target) DB Batch Insert ]---------------------------------");
				
				dataSourceTransactionManager.commit(status);
				// commit
			} catch (Exception e) {
				LOG.debug("+-[ Exception ]---------------------------------["+e.getLocalizedMessage()+"]");
				throw new Exception(e);
			}			
		} catch (Exception e2) {
			LOG.debug("+-[ Exception ]---------------------------------["+e2.getLocalizedMessage()+"]");
			throw new Exception(e2);
		}			
	}
	
	
	@SuppressWarnings("rawtypes")
	public void setFirst(List<Map> dataList,List<Map> dataList2) {
		for(Map record : dataList2) {
			for(Map delCondition : dataList) {

				String strInkey1	=	(String) record.get("pt_id");
				String strInkey2	=	(String) record.get("point_name");
				String strInkey3	=	(String) record.get("date_occurred");
				String strInkey4	=	(String) record.get("time_occurred");


				String strkey1	=	(String) delCondition.get("pt_id");
				String strkey2	=	(String) delCondition.get("point_name");
				String strkey3	=	(String) delCondition.get("date_occurred");
				String strkey4	=	(String) delCondition.get("time_occurred");

				String skipFlag	= "";
				if(delCondition.get("skip")!=null)
					skipFlag	=	delCondition.get("skip").toString();
				
				String skipFlag2	= "";
				if(record.get("skip")!=null)
					skipFlag2	=	record.get("skip").toString();				
				
				if(skipFlag2.contentEquals(""))
					record.put("skip", "O");
				
				if(strInkey1.equals(strkey1)
						&& strInkey2.equals(strkey2)
						&& strInkey3.equals(strkey3)
						&& strInkey4.equals(strkey4) && (delCondition != record) && skipFlag.equals("")) {
					delCondition.put("skip", "S");
				}
			}
		}
	}

	public void initDirectory(String checkFolder) {
		File destdir = new File(checkFolder);
		
		if(!destdir.exists()){
            destdir.mkdirs(); //디렉토리가 존재하지 않는다면 생성
        }

		LOG.debug("+-[ watchService.create CHK ]------::" +checkFolder+File.separator+CONSTANT_CHK_DIR+File.separator);		
		File destdirSubChk = new File(checkFolder+File.separator+CONSTANT_CHK_DIR+File.separator);
		if(!destdirSubChk.exists()){
			destdirSubChk.mkdirs(); //디렉토리가 존재하지 않는다면 생성
        }
		
		LOG.debug("+-[ watchService.create CHK ]------::" +checkFolder+File.separator+CONSTANT_FAIL_DIR+File.separator);		
		File destdirSubFail = new File(checkFolder+File.separator+CONSTANT_FAIL_DIR+File.separator);
		if(!destdirSubFail.exists()){
			destdirSubFail.mkdirs(); //디렉토리가 존재하지 않는다면 생성
        }
		
	}

	public synchronized void fileCheckTail(String checkFolder,String fileName) {
		File file =	null;
		RandomAccessFile fileHandler 	= null;
		File fileCheckpoint				= null;
        RandomAccessFile fileHandlerC 	= null;
        String chkSubDir	=	CONSTANT_CHK_DIR;
		try {
			file = new File(checkFolder+fileName);
			
			if(!file.exists()) {
				LOG.debug("+-[ watchService.fileCheckTail ]------:: ALREADY MOVE FILE TO FAIL OR COMPLTED>>" +fileName);
				return;
			}
			
            fileHandler 	= 	new RandomAccessFile(file, "r");	
            	
            long lineSize	=	getLineNumber(fileHandler);     
            fileHandler.seek(0);
            
            long tailpositionline	=	0;
            long tailfileSize		=	0;
            String errorMsg			=	"";
            
			fileCheckpoint = new File(checkFolder+chkSubDir+File.separator+fileName+CONSTANT_EXTEND_CHK); 
            boolean isExists = fileCheckpoint.exists();
            
            if(isExists) {
            	fileHandlerC 	= 	new RandomAccessFile(fileCheckpoint, "r");	
            	String readInfo	=	getLastNonBlankLine(fileCheckpoint);
            	if(readInfo != null && !readInfo.contentEquals("")) {
            		if(readInfo.contains(",")) {
	            		String[] sizeNLine	=	readInfo.split(",");
	            		tailfileSize	=	Long.parseLong(sizeNLine[0]);
	            		tailpositionline	=	Long.parseLong(sizeNLine[1]);
	            		if(sizeNLine.length > 2)
	            			errorMsg	=	sizeNLine[2];
            		}else {
            			errorMsg	=	readInfo;
            		}
            	}
            }
            
            // error case move to fail
            if(!errorMsg.equals("") && errorMsg != null) {
            	LOG.debug("+-[ watchService.fileCheckTail ]------:: ERROR MOVE["+errorMsg+"] >> " +fileName);
            	String errorFailMsg	=	errorMsg;
            	// .checkpoint 저장
            	setTraceFiles(fileCheckpoint,file.length(),Long.toString(lineSize),errorFailMsg);
            	File fileFailDir =	makeFailFile(checkFolder,CONSTANT_FAIL_DIR,fileName);

            	// .fail 저장
            	// close & move            	
            	setTraceFiles(fileFailDir,file.length(),Long.toString(lineSize),errorFailMsg);
            	fileHandler.close();
            	moveFile(checkFolder+fileName, checkFolder+CONSTANT_FAIL_DIR+File.separator+fileName);
            	
            }else if(file.length() == tailfileSize) {
            	LOG.debug("+-[ watchService.fileCheckTail ]------:: OLD COMPLETE MOVE >> " + fileName);
        		// 3일전 파일이고, checkpoint 의 사이즈와 파일 사이즈가 같으면,혹은 에러인경우 move 처리
        		if(!isBetween(fileName)) {
                	// close & move
        			fileHandler.close();
                	moveFile(checkFolder+fileName, checkFolder+"COMPLETED"+File.separator+fileName);
        		}
        	}else {
            	LOG.debug("+-[ watchService.readPosition call ]------:: seek init in readPosition ");
        		long position	=	readPosition(fileHandler,tailpositionline);
            	LOG.debug("+-[ watchService.fileCheckTail ]------:: READ START["+position+","+tailpositionline+"] >> " + fileName);
        		try {
        			boolean isSuccess	=	dataMergeIntoHr(fileHandler,position);
	        		if(isSuccess) {
	        			appendToFileFileWriter(fileCheckpoint,file.length()+","+lineSize);
	        		}
        		}catch(Exception ex) {
        			if(ex.getLocalizedMessage().contains("DBConnection_ERROR_RETRY")) {
        				LOG.debug("+-[ watchService.fileCheckTail ]------:: DATABASE CONNECTION FAIL");
        				// retry
        				return;
        			}else {
                    	String errorFailMsg	=	ex.getLocalizedMessage();
                    	// .checkpoint 저장
                    	setTraceFiles(fileCheckpoint,file.length(),Long.toString(lineSize),errorFailMsg);
                    	File fileFailDir =	makeFailFile(checkFolder,CONSTANT_FAIL_DIR,fileName);

                    	// .fail 저장
                    	// close & move                    	
                    	setTraceFiles(fileFailDir,file.length(),Long.toString(lineSize),errorFailMsg);
                    	fileHandler.close();
                    	moveFile(checkFolder+fileName, checkFolder+CONSTANT_FAIL_DIR+File.separator+fileName);
        			}
        		}
        	}
            
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			if(fileHandler!=null )
				try {
					fileHandler.close();
				} catch (IOException e) {
					e.printStackTrace(System.out);
				}
			if(fileHandlerC!=null )
				try {
					fileHandlerC.close();
				} catch (IOException e) {
					e.printStackTrace(System.out);
				}
		}
	}
	
    // getlast line
    private static long getLineNumber(RandomAccessFile rf) throws IOException {  
    	long recnum = 0; 
    	while ((rf.readLine()) != null) {
    		recnum++;
    	}
        return recnum;  
    }   
    
    private boolean dataMergeIntoHr(RandomAccessFile rf,long tailPosition) throws Exception {
    	try {
    		long recnum = 0;  
    		String temp;  
    		rf.seek(tailPosition);
    		String sendBuffer	=	"";
            LOG.debug("+-[ watchService.readPosition sendBuffer]------:: findLine1 >> " + sendBuffer);        
            
            int i =0;
    		while((temp = rf.readLine()) != null)  
    		{  
    			
    			if(i < 11) {
    			//	printEncoding(temp);
    				i++;
    			}

    			
    			//String tempEucKr = new String(temp.getBytes("iso-8859-1"));
//    			String tempEucKr = new String(temp.getBytes("euc-kr"));
    			String tempEucKr = new String(temp.getBytes("ISO-8859-1"), "MS949");
    			
    			if(sendBuffer.contentEquals("")) {
    				sendBuffer	=	tempEucKr;
    			}else {
    				sendBuffer	=	sendBuffer+"\n"+ tempEucKr;
    			}

    			if(((++recnum) == ibatchSize))  
    			{  
    				//System.out.println("tail Line " + recnum + " : " + sendBuffer);
    				process(sendBuffer);

    				sendBuffer	=	"";
    				recnum = 0;
    			}
    		}
            LOG.debug("+-[ watchService.readPosition sendBuffer]------:: findLine2 >> " + sendBuffer);        
    		if(!sendBuffer.contentEquals("")) {
				process(sendBuffer);
    		}
    	} catch (Exception e){
    		e.printStackTrace(System.out);
    		throw e;
    	}
    	return true;
    }    
    
    private static long readPosition(RandomAccessFile rf,long findLine) throws IOException {  
        long recnum = 1;  
        String temp;  
        
        rf.seek(0);

        // file write sleep 
        if( rf.length() == 0) {
            rf.seek(0);
        	try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
        }
        
     //   temp = rf.readLine();
        if(findLine == 0) return rf.getFilePointer();
        
        while((temp = rf.readLine()) != null)  
        {  
        	
            if(((recnum++) == findLine))  
            {  
                break;  
            }  
        }  
        LOG.debug("+-[ watchService.readPosition find line recnum]------:: return pointer >> " + rf.getFilePointer());        
        return rf.getFilePointer();  
    }
    
    
    private static void appendToFileFileWriter(File file, String content) throws IOException {
//    		System.out.println("name is["+file.getAbsolutePath());
    	
    		if(!file.exists())
    			file.createNewFile();
    	
            try (FileWriter fw = new FileWriter(file, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {

                    bw.write(content);
                    bw.newLine();   // add new line, System.lineSeparator()

            }
    }    
    

    public String getLastNonBlankLine(File file) throws Exception
    {
    	RandomAccessFile fileHandler = null;
    	try
    	{
    		fileHandler = new RandomAccessFile(file, "r");
    		long fileLength = file.length() - 1;
    		StringBuilder sb = new StringBuilder();
    		for (long filePointer = fileLength; filePointer != -1; filePointer--)
    		{
    			fileHandler.seek(filePointer);
    			int readByte = fileHandler.readByte();

    			if (readByte == 0xA)
    			{
    				if (filePointer == fileLength)
    				{
    					continue;
    				}
    				else
    				{
    					if (!StringUtils.isEmpty(sb.toString().trim()))
    						break;
    				}
    			}
    			else if (readByte == 0xD)
    			{
    				if (filePointer == fileLength - 1)
    				{
    					continue;
    				}
    				else
    				{
    					if (!StringUtils.isEmpty(sb.toString().trim()))
    						break;
    				}
    			}
    			sb.append((char) readByte);
    		}

    		String lastLine = sb.reverse().toString();
    		return lastLine;
    	}
    	catch (java.io.FileNotFoundException e)
    	{
    		throw e;
    	}
    	catch (java.io.IOException e)
    	{
    		throw e;
    	}
    	finally
    	{
    		fileHandler.close();
    	}
    } 

	public static List<String> readLines(File file) throws Exception {
		try {
			if(!file.exists()) {
				throw new Exception(String.format("The file to read does not exist.[%s]",file.getPath()));
			}
			if(!file.isFile()){
				throw new Exception(String.format("Invalid file name. not a file.[%s]",file.getPath()));
			} 
			return FileUtils.readLines(file,"UTF-8");
		} catch (IOException e) {
			LOG.error("[CommonUtils] >> readLines :: Error > {}", CommonUtils.getPrintStackTrace(e));
			throw new Exception(
			String.format("File read failed. IOException occurred.[%s]", file.getPath()), e);
		}
	}	
	

	/**
	 * 
	 * <pre>
	 * 1. 개요 : Stack trace Object를 String으로 변환
	 * 2. 처리내용 : 
	 * </pre>
	 * @Method Name : getPrintStackTrace
	 * @date : 2019. 11. 1.
	 * @author : A931744
	 * @history : 
	 *	-----------------------------------------------------------------------
	 *	변경일				작성자						변경내용  
	 *	----------- ------------------- ---------------------------------------
	 *	2019. 11. 1.		A931744				최초 작성 
	 *	-----------------------------------------------------------------------
	 * 
	 * @param clsObject stack trace object
	 * @return
	 */
	public static String getPrintStackTrace(Object clsObject) {
		ByteArrayOutputStream clsOutStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(clsOutStream);
		((Throwable) clsObject).printStackTrace(printStream);
		return clsOutStream.toString();
	}
	
	
	public static boolean moveFile(String srcFile, String tgtFile) {
		try {
			File file = FileUtils.getFile(srcFile);
			File fileToMove = FileUtils.getFile(tgtFile);
			if (fileToMove.exists()) {
				  org.apache.commons.io.FileUtils.deleteQuietly(fileToMove);
			}
			FileUtils.moveFile(file, fileToMove);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static File makeFailFile(String checkFolder,String sub,String fileName) {
        RandomAccessFile fileHandlerC = null;
        String failubDir	=	sub;
		try {
			File fileCheckpoint =	null;
			fileCheckpoint = new File(checkFolder+failubDir+File.separator+fileName+"."+failubDir.toLowerCase());
			return fileCheckpoint;
		}catch(Exception ex) {
			ex.printStackTrace(System.out);
		}
		return null;
	}

	private boolean isBetween(String fileName) {
		try {
			String dateStr		=	getDayFromFileName(fileName);
			String dateBefore3	=	getDayAgoDate(-3);
			if(Integer.parseInt(dateStr) >= Integer.parseInt(dateBefore3)) {
				return true;
			}
		}catch(Exception ex) {
			ex.printStackTrace(System.out);
		}
		return false;
	}	


	private String getDayFromFileName(String fileName) {
		if(fileName.length() >= 8) {
			return fileName.substring(0,8);
		}else {
			return "19000101";
		}
	}	

	private String getDayAgoDate(int period) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, period);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		return formatter.format(cal.getTime());
	}
	
	private void setTraceFiles(File fileCheckpoint,long fileLength,String lineSize,String errorMsg) {
		try {
			if(errorMsg != null && !errorMsg.contentEquals("")) {
				appendToFileFileWriter(fileCheckpoint,fileLength+","+lineSize+","+errorMsg);
			}else {
				appendToFileFileWriter(fileCheckpoint,fileLength+","+lineSize);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
	}
	
	public static boolean toBeDelay(RandomAccessFile rf) {
		FileChannel channel = rf.getChannel();
		FileLock lock =	null;
		try {
			lock = channel.lock();
		    lock = channel.tryLock();
		    System.out.print("file is not locked");
		} catch (OverlappingFileLockException e) {
		    System.out.print("file is locked");
		    return true;
		} catch (Exception e) {
		    System.out.print("file is locked");
		    return true;
		} finally {
			if(lock!=null) {
				try {
					lock.release();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	
}
