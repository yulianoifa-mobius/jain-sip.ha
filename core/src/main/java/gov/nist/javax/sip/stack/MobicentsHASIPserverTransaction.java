/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package gov.nist.javax.sip.stack;

import gov.nist.core.StackLogger;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.message.Request;

import org.mobicents.ha.javax.sip.ClusteredSipStack;
import org.mobicents.ha.javax.sip.cache.SipCacheException;

/**
 * @author jean.deruelle@gmail.com
 *
 */
public class MobicentsHASIPserverTransaction extends MobicentsSIPServerTransaction {

	String localDialogId;
	
	public MobicentsHASIPserverTransaction(SIPTransactionStack sipStack,
			MessageChannel newChannelToUse) {
		super(sipStack, newChannelToUse);
	}

	@Override
	protected void map() {
		super.map();
//		// store the tx when the transaction is mapped
//		try {
//			((ClusteredSipStack)sipStack).getSipCache().putServerTransaction(this);
//		} catch (SipCacheException e) {
//			sipStack.getStackLogger().logError("problem storing server transaction " + transactionId + " into the distributed cache", e);
//		}
	}

	public Map<String, Object> getMetaDataToReplicate() {
		Map<String,Object> transactionMetaData = new HashMap<String,Object>();
		
		transactionMetaData.put("req", getOriginalRequest().toString());
		if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
			sipStack.getStackLogger().logDebug(transactionId + " : original request " + getOriginalRequest());
		}
		if(dialogId != null) {
			transactionMetaData.put("did", dialogId);
			if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
				sipStack.getStackLogger().logDebug(transactionId + " : dialog Id " + dialogId);
			}
		} else if(localDialogId != null) {
			transactionMetaData.put("did", localDialogId);
			if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
				sipStack.getStackLogger().logDebug(transactionId + " : dialog Id " + localDialogId);
			}
		}
		if(getState() != null) {
			transactionMetaData.put("cs", Integer.valueOf(getState().getValue()));
			if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
				sipStack.getStackLogger().logDebug(transactionId + " : current state " + getState());
			}
		}
		transactionMetaData.put("ct", getMessageChannel().getTransport());
		if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
			sipStack.getStackLogger().logDebug(transactionId + " : message channel transport " + getTransport());
		}
		transactionMetaData.put("cip", getMessageChannel().getPeerInetAddress());
		if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
			sipStack.getStackLogger().logDebug(transactionId + " : message channel ip " + getMessageChannel().getPeerInetAddress());
		}
		transactionMetaData.put("cp", Integer.valueOf(getMessageChannel().getPeerPort()));
		if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
			sipStack.getStackLogger().logDebug(transactionId + " : message channel ip " + getMessageChannel().getPeerPort());
		}
		transactionMetaData.put("mp", Integer.valueOf(getMessageChannel().getPort()));
		if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
			sipStack.getStackLogger().logDebug(transactionId + " : message channel ip " + getMessageChannel().getPort());
		}
		
		return transactionMetaData;
	}		
	
	@Override
	public void sendMessage(SIPMessage message) throws IOException {
		final SIPResponse response = (SIPResponse) message;
		if(response != null && Request.INVITE.equals(getMethod()) && response.getStatusCode() > 100 && response.getStatusCode() < 200) {
			this.localDialogId = response.getDialogId(true);
			if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
				sipStack.getStackLogger().logDebug(transactionId + " : local dialog Id " + localDialogId);
			}
			// store the tx when the response will be sent
			try {
				((ClusteredSipStack)sipStack).getSipCache().putServerTransaction(this);
			} catch (SipCacheException e) {
				sipStack.getStackLogger().logError("problem storing server transaction " + transactionId + " into the distributed cache", e);
			}
		}
		super.sendResponse(response);
	}

	public Object getApplicationDataToReplicate() {
		return null;
	}

	public void setMetaDataToReplicate(Map<String, Object> transactionMetaData,
			boolean recreation) throws PeerUnavailableException, ParseException {
		String originalRequestString = (String) transactionMetaData.get("req");
		if(originalRequestString != null) {
			final SIPRequest origRequest = (SIPRequest) SipFactory.getInstance().createMessageFactory().createRequest(originalRequestString);			
			setOriginalRequest(origRequest);
			if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
				sipStack.getStackLogger().logDebug(transactionId + " : original Request " + originalRequest);
			}
		}
		String dialogId = (String) transactionMetaData.get("did");
		if(dialogId != null) {
			SIPDialog sipDialog = sipStack.getDialog(dialogId);
			setDialog(sipDialog, dialogId);
			if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
				sipStack.getStackLogger().logDebug(transactionId + " : dialog Id " + dialogId + " dialog " + sipDialog);
			}
		}
		Integer state = (Integer) transactionMetaData.get("cs");
		if(state != null) {
			setState(state);
			if (sipStack.getStackLogger().isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
				sipStack.getStackLogger().logDebug(transactionId + " : state " + getState());
			}
		}		
	}

	public void setApplicationDataToReplicate(Object transactionAppData) {
		
	}
}