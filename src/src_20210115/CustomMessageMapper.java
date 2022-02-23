/*
 * Copyright Hyundai AutoEver.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Hyundai AutoEver. ("Confidential Information").
 */
package com.xconnect.eai.external.HmcEaiConnect; 

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.Exchange;

import com.xconnect.eai.server.common.constant.SystemCommonConstants;
import com.xconnect.eai.server.common.exception.XConnectEaiServerException;
import com.xconnect.eai.server.common.util.CommonUtils;
import com.xconnect.eai.server.common.vo.InterfaceRequestMessage;

public class CustomMessageMapper {
	private static final Logger LOG = LoggerFactory.getLogger(CustomMessageMapper.class);
	/**
	 * <pre>
	 * 1. 개요 : 서버 Adapter 앞단에서 선처리, ifid등 안들어오는경우 직접 작성
	 * 2. 처리내용 : IF_ID 값을 들어온 xml 을 보고 라우팅 할부분으로 작성
	 * </pre>
	 * @Method Name : process
	 * @date : 2020. 12. 18.
	 * @author : 6801740
	 * @history : 
	 * <pre>
	 *	-----------------------------------------------------------------------
	 *	변경일	         작성자                변경내용  
	 *	----------- ------------------- ---------------------------------------
	 *	2020. 12. 18.		6801740				최초 작성 
	 *	2021. 01. 15.		6801740				HMC 수신 인터페이스 맵핑 등록(전체) 
	 *	-----------------------------------------------------------------------
	 * <pre>
	 *
	 * @param objectExchange
	 * @param inSoapRawMap
	 * @throws Exception
	 */ 	
	public void process(Object objectExchange, Map<String, Object> inSoapRawMap) throws Exception {
		try {
			/*
			 * Interface ID 만 매핑
			 */
			Map<String, Object> soapRawMap 		= (Map<String, Object>) inSoapRawMap;
			Map<String, Object> soapHeaderMap	= null;
			try {
				soapHeaderMap	= (Map<String, Object>)((Map)soapRawMap.get("Envelope")).get("Header");
			} catch(Exception e) {}
			Map<String, Object> soapBodyMap = (Map<String, Object>)((Map)soapRawMap.get("Envelope")).get("Body");
			if(soapHeaderMap == null)
				soapHeaderMap 		= new HashMap<String, Object>();
			
			
			//{Envelope={Header={ifVer=v001, ifDateTime=20200601101100, ifTrackingId=0c7278c5-9346-7117-b77d-soap_mobis_0400119, ifReceiverGrp=KR, ifReceiver=MOBIS, ifId=IF_KR_0007, ifSenderGrp=KR, ifSender=}, Body={ifVer=v001, ifDateTime=20200601101100, ifTrackingId=0c7278c5-9346-7117-b77d-soap_mobis_0400119, ifReceiverGrp=KR, ifReceiver=MOBIS, ifId=IF_KR_0007, ifSenderGrp=KR, ifSender=}, xmlns:soapenv=http://schemas.xmlsoap.org/soap/envelope/, xmlns:soap=http://10.10.152.252:7072/services/receive/http://10.7.149.46:7072/eai/SOAP_TEST_0002Service}}
			
			/*
			 * HR 예외처리
			 */
			if(CommonUtils.getValueByDepthKey(soapBodyMap, "request.Header.ifId", "") !=null) {
				String ifid	= (String) CommonUtils.getValueByDepthKey(soapBodyMap, "request.Header.ifId", "");
				String ifVer	= (String) CommonUtils.getValueByDepthKey(soapBodyMap, "request.Header.ifVer", "");
				soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,ifid);
				soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,ifVer);
			}else {

				/*
				 * Start call method
				 * unzip data
				 */
				Exchange exchange	=	(Exchange) objectExchange;
				exchange.setProperty("INIT_METHOD", "startProcess");

				/*exchange.setProperty("sample", "sample");*/

				// 0: soapHeaderMap 값을 바탕으로 기본갑셋팅
				if(CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.IFID", "")!=null) {
					String ifid	= (String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.IFID", "");
					String documenttype	=	(String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.DOCUMENTTYPE", "");
					exchange.setProperty("Compressed"	, "0");
					exchange.setProperty("ExtraVariables", "");
					exchange.setProperty("RequestId"	, "0");
					
					String zip				= 	(String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.ZIP", "");
					String datatype			= 	(String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.DATATYPE", "");
					exchange.setProperty("ZIP", zip.toUpperCase());
					exchange.setProperty("DATATYPE", datatype);

					
					/*
					 * 12.30 매핑 부분
						PPH002	NKPRDZHPPTWOBP	IF_KR_HERP_GERP_PPH0200	ALC-II코드정보
						PPH003	NKPRDZHPPTWOPP1	IF_KR_HERP_GERP_PPH0300	ALC-II코드정보PART
						PPH004	NKPRDZHPPTWOPP2	IF_KR_HERP_GERP_PPH0400	ALC-II코드정보PART2
						PPH005	NKPRDZHPPTWOQP	IF_KR_HERP_GERP_PPH0500	ALC-II코드정보Q-PART
						PPH006	NKPRDZHPPT2208	IF_KR_HERP_GERP_PPH0600	ALC-II칼럼정보접수
						PPH010	NKPRDZHPPTVMSTCP	IF_KR_HERP_GERP_PPH1000	실시간VIN정보수신
						PPH032	NKPRDZHPPTWOHD	IF_KR_HERP_GERP_PPH3200	WorkOrder Header
						PPH033	NKPRDZHPPTWOCL	IF_KR_HERP_GERP_PPH3300	WorkOrder Color별
						PPH034	NKPRDZHPPTWOUP	IF_KR_HERP_GERP_PPH3400	WorkOrder U-Part
						PPH035	NKPRDZHPPTWOCP	IF_KR_HERP_GERP_PPH3500	WorkOrder C-Part
						PPH036	NKPRDZHPPTW219	IF_KR_HERP_GERP_PPH3600	Work Order 219 Option
						PPH037	NKPRDZHPPTVMST	IF_KR_HERP_GERP_PPH3700	차량마스터 IF
						PPH048	NKPRDZHPPT3202	IF_KR_HERP_GERP_PPH4800	서열계획확정(WorkOrder&FSC용)
						PPH049	NKPRDZHPPT3203	IF_KR_HERP_GERP_PPH4900	서열계획확정(ALCCode용)
						PPH050	NKPRDZHPPT3204	IF_KR_HERP_GERP_PPH5000	서열계획확정(ALCCodeII용)
						PPH051	NKPRDZHPPT3205	IF_KR_HERP_GERP_PPH5100	서열계획확정(사용자정의용)
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_0002");
					 * 
					 */
					
					/* MD모듈
					 */
					if(ifid.equals("MDH001")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH0100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2009");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH002")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH0200");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2904");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH003")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH0300");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2908");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH006")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH0600");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2006");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH007")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH0700");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2013");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH008")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH0800");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2001");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH009")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH0900");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2204");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH010")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH1000");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2219");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH011")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH1100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2116");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH013")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH1300");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT9000");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH017")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH1700");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2022");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH018")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH1800");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2024");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH019")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH1900");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2204");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH020")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2000");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2007");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH021")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2032");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH022")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2200");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT9001");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH023")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2300");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT9002");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH024")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2400");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2215");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH025")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2500");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2214");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH026")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2600");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2906");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH028")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH2800");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2213");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH030")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH3000");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2203");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MDH031")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MDH3100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKBOMZHMDT2246");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					/* PP모듈
					 */
					}else if(ifid.equals("PPH002")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH0200");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOBP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH003")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH0300");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOPP1");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH004")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH0400");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOPP2");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH005")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH0500");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOQP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH006")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH0600");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT2208");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH007")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH0700");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWEOL");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH010")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH1000");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTVMST");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH011")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH1100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTVMCHCP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH016")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH1600");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTVLOGCP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH021")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH2100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT3300CP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH025")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH2500");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT0021");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH032")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3200");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOHD");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH033")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3300");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOCL");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH034")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3400");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOUP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH035")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3500");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOCP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH036")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3600");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTW219");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH037")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3700");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTVMST");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"I");
					}else if(ifid.equals("PPH038")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3800");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT3207");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH039")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH3900");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH044")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH4400");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH045")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH4500");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH047")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH4700");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTBLOGCP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH048")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH4800");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT4202");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH049")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH4900");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT4203");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH050")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5000");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT4204");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH051")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT4205");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH052")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5200");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPT4206");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH053")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5300");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH054")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5400");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH055")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5500");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH056")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5600");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH057")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5700");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH058")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5800");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH059")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH5900");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKPRDZHPPTWOEP");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH060")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH6000");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH061")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH6100");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH062")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH6200");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("PPH065")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_PPH6500");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "mkl_not_ServiceId");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					/* MM모듈
					 */
					}else if(ifid.equals("MMH006")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MMH0600");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKINVZHMMT0069");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else if(ifid.equals("MMH012")) {
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_HERP_GERP_MMH1200");
						soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFVER,"v001");
						exchange.setProperty("ServiceId"	, "NKINVSHIPPARTNERRECV");
						exchange.setProperty("ParameterTable"	, CommonUtils.getProperty(exchange, "ServiceId").substring(5)+"IF");
					}else {
						String errorMssage = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://10.182.62.15:7011/services/receive\" xmlns:hd=\"http://10.182.62.15:7011/services/receive/headers\">\r\n" + 
								"   <soap:Header/>\r\n" + 
								"   <soap:Body>\r\n" + 
								"      <ws:ToHKMCGDIHResponse>\r\n" + 
								"         <IFRESULT>E</IFRESULT>\r\n" + 
								"         <IFMESSAGE>@MESSAGE@</IFMESSAGE>\r\n" + 
								"         <ZIP>TEXT</ZIP>\r\n" + 
								"         <DATATYPE>XML</DATATYPE>\r\n" + 
								"         <OUT_DATA>\r\n" + 
								"            <outData>\r\n" + 
								"               <TBLRETURN/>\r\n" + 
								"            </outData>\r\n" + 
								"         </OUT_DATA>\r\n" + 
								"      </ws:ToHKMCGDIHResponse>\r\n" + 
								"   </soap:Body>\r\n" + 
								"</soap:Envelope>";
						
						exchange.setProperty("CUSTOM_ERROR_MSGFORMAT", errorMssage);
						exchange.setProperty("CUSTOM_ERROR_CONTENTTYPE", "xml");
						throw new XConnectEaiServerException(SystemCommonConstants.EAI_SERVER_ERROR_PREFIX + " - Message생성에 실패하였습니다.", XConnectEaiServerException.REQUEST_BODY_INVALID);
					}
					
					// local test
					//soapHeaderMap.put(SystemCommonConstants.HEADER_KEY_IFID,"IF_KR_0002");
				}

			}
			
			((Map)soapRawMap.get("Envelope")).put("Header",soapHeaderMap);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}	
	
	
	/**
	 * <pre>
	 * 1. 개요 :  Start 지점에서 초기화 처리
	 * 2. 처리내용 : 초기화 내용이 필요없으면 안해도 됨, 들어온 데이터가 압축등 선처리가 필요한경우 처리
	 *            인터페이스 ID 이외의 ifSender 등 기본정보 정의
	 * </pre>
	 * @Method Name : startProcess
	 * @date : 2020. 12. 18.
	 * @author : 6801740
	 * @history : 
	 * <pre>
	 *	-----------------------------------------------------------------------
	 *	변경일	         작성자                변경내용  
	 *	----------- ------------------- ---------------------------------------
	 *	2020. 12. 18.		6801740				최초 작성 
	 *	-----------------------------------------------------------------------
	 * <pre>
	 *
	 * @param objectExchange
	 * @param inSoapRawMap
	 * @throws Exception
	 */ 	
	public void startProcess(Object objectExchange, Map<String, Object> inSoapRawMap) throws Exception {
		try {
//			Exchange exchange	=	(Exchange) objectExchange;
//			InterfaceRequestMessage interfaceRequestMessage = exchange.getIn().getBody(InterfaceRequestMessage.class);
//			Map<String, Object> soapBodyMap = interfaceRequestMessage.getBody();			
//			// file 로 관리 필요
//			String receiveData	=	"";
//			String company			= (String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.COMPANY", "");
//			String sender			= (String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.SENDER", "");
//			String ifid				= (String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.IFID", "");
//			String documenttype		=	(String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.DOCUMENTTYPE", "");
//			String target_system	=	(String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.TARGET_SYSTEM", "");
//			String zip				= 	(String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.ZIP", "");
//			String datatype			= 	(String) CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.DATATYPE", "");
//			
//			exchange.setProperty("ZIP", zip.toUpperCase());
//			exchange.setProperty("DATATYPE", datatype);
//			
//			exchange.setProperty("ZIPDATATYPE", zip.toUpperCase()+datatype.toUpperCase());
//			if(CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.IN_DATA.InputData", "") != null) {
//				//Object inData	=	CommonUtils.getValueByDepthKey(soapBodyMap, "ToHKMCGDIH.IN_DATA.InputData", "");
//				//receiveData			=	CommonUtils.base64ZipConversionXML(zip.toUpperCase()+datatype.toUpperCase(),inData);
//				
//				// Canias Root 요청시 변경 
//				//receiveData	=	"<ROOT>"+receiveData+"</ROOT>";
//			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}	
	
	
	/**
	 * <pre>
	 * 1. 개요 :  data 필드가 다른경우 매핑 to hkmc
	 * 2. 처리내용 : 초기화 내용이 필요없으면 안해도 됨, 들어온 데이터가 압축등 선처리가 필요한경우 처리
	 *            인터페이스 ID 이외의 ifSender 등 기본정보 정의
	 * </pre>
	 * @Method Name : sendProcess
	 * @date : 2020. 12. 18.
	 * @author : 6801740
	 * @history : 
	 * <pre>
	 *	-----------------------------------------------------------------------
	 *	변경일	         작성자                변경내용  
	 *	----------- ------------------- ---------------------------------------
	 *	2020. 12. 18.		6801740				최초 작성 
	 *	-----------------------------------------------------------------------
	 * <pre>
	 *
	 * @param objectExchange
	 * @param inSoapRawMap
	 * @throws Exception
	 */ 	

	public void sendProcess(Object objectExchange, Map<String, Object> inSoapRawMap) throws Exception {
	
		try {
	         Exchange exchange   =   (Exchange) objectExchange;
	         InterfaceRequestMessage interfaceRequestMessage = exchange.getIn().getBody(InterfaceRequestMessage.class);         
	         inSoapRawMap = interfaceRequestMessage.getBody();
	         
	         exchange.setProperty("COMPANY", "G10");
	         exchange.setProperty("WERKS", "GV1");
	         exchange.setProperty("ERNAM", "HERPMM");
	         
 	         LOG.debug("+-[ sendProcess ] test v0.mkl100");
 	         LOG.debug("+-[ sendProcess ] ---------------------------------");
	         LOG.debug("+-[ sendProcess ] inSoapRawMap = [" + inSoapRawMap + "]");
 	         LOG.debug("+-[ sendProcess ] ---------------------------------");
	         LOG.debug("+-[ sendProcess ] exchange = [" + exchange + "]");
	         LOG.debug("+-[ sendProcess ] ---------------------------------");
	         
	      } catch (Exception e) {
	         e.printStackTrace(System.out);
	      }
	   }	
	
	
	
	
	
	
	
	/**
	 * <pre>
	 * 1. 개요 :  data 필드가 다른경우 매핑 from hkmc
	 * 2. 처리내용 : 초기화 내용이 필요없으면 안해도 됨, 들어온 데이터가 압축등 선처리가 필요한경우 처리
	 *            인터페이스 ID 이외의 ifSender 등 기본정보 정의
	 * </pre>
	 * @Method Name : receiveProcess
	 * @date : 2020. 12. 18.
	 * @author : 6801740
	 * @history : 
	 * <pre>
	 *	-----------------------------------------------------------------------
	 *	변경일	         작성자                변경내용  
	 *	----------- ------------------- ---------------------------------------
	 *	2020. 12. 18.		6801740				최초 작성 
	 *	-----------------------------------------------------------------------
	 * <pre>
	 *
	 * @param objectExchange
	 * @param inSoapRawMap
	 * @throws Exception
	 */ 	
	public void receiveProcess(Object objectExchange, Map<String, Object> inSoapRawMap) throws Exception {
		try {

		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}		
	
//	// data tag를 인터페이스 타입에 맞게 압축,변환
//	public String converToHmcDataType(Object objectExchange,Map dummy) throws Exception {
//		Exchange exchange	=	(Exchange) objectExchange;
//		InterfaceRequestMessage dataBody = (InterfaceRequestMessage) exchange.getIn().getBody();
//		
//		// 초기 데이터는 xml 임
////		String dataType	=	CommonUtils.getProperty(exchange, "DATATYPE");
//		String dataType	= dataBody.getIfReceiverGrp()+dataBody.getIfReceiver();
//		Map	data		=	(Map) CommonUtils.getValueByDepthKey(dataBody.getBody(), "request.T_DATA", "");
//		
//		String srcString	=	CommonUtils.MapToXml(data);
//		srcString			=	CommonUtils.base64ZipConversionZip(dataType,srcString);
//		CommonUtils.setValueAtDepthKey(dataBody.getBody(), "request.T_DATA", srcString,"");
//		
//		return "";
//	}
	
}
