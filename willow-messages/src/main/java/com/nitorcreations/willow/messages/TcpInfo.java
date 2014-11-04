package com.nitorcreations.willow.messages;

public class TcpInfo extends AbstractMessage {
	int tcpInboundTotal;
	int tcpOutboundTotal;
	int allInboundTotal;
	int allOutboundTotal;
	int tcpEstablished;
	int tcpSynSent;
	int tcpSynRecv;
	int tcpFinWait1;
	int tcpFinWait2;
	int tcpTimeWait;
	int tcpClose;
	int tcpCloseWait;
	int tcpLastAck;
	int tcpListen;
	int tcpClosing;
	int tcpIdle;
	int tcpBound;
	public int getTcpInboundTotal() {
		return tcpInboundTotal;
	}
	public void setTcpInboundTotal(int tcpInboundTotal) {
		this.tcpInboundTotal = tcpInboundTotal;
	}
	public int getTcpOutboundTotal() {
		return tcpOutboundTotal;
	}
	public void setTcpOutboundTotal(int tcpOutboundTotal) {
		this.tcpOutboundTotal = tcpOutboundTotal;
	}
	public int getAllInboundTotal() {
		return allInboundTotal;
	}
	public void setAllInboundTotal(int allInboundTotal) {
		this.allInboundTotal = allInboundTotal;
	}
	public int getAllOutboundTotal() {
		return allOutboundTotal;
	}
	public void setAllOutboundTotal(int allOutboundTotal) {
		this.allOutboundTotal = allOutboundTotal;
	}
	public int getTcpEstablished() {
		return tcpEstablished;
	}
	public void setTcpEstablished(int tcpEstablished) {
		this.tcpEstablished = tcpEstablished;
	}
	public int getTcpSynSent() {
		return tcpSynSent;
	}
	public void setTcpSynSent(int tcpSynSent) {
		this.tcpSynSent = tcpSynSent;
	}
	public int getTcpSynRecv() {
		return tcpSynRecv;
	}
	public void setTcpSynRecv(int tcpSynRecv) {
		this.tcpSynRecv = tcpSynRecv;
	}
	public int getTcpFinWait1() {
		return tcpFinWait1;
	}
	public void setTcpFinWait1(int tcpFinWait1) {
		this.tcpFinWait1 = tcpFinWait1;
	}
	public int getTcpFinWait2() {
		return tcpFinWait2;
	}
	public void setTcpFinWait2(int tcpFinWait2) {
		this.tcpFinWait2 = tcpFinWait2;
	}
	public int getTcpTimeWait() {
		return tcpTimeWait;
	}
	public void setTcpTimeWait(int tcpTimeWait) {
		this.tcpTimeWait = tcpTimeWait;
	}
	public int getTcpClose() {
		return tcpClose;
	}
	public void setTcpClose(int tcpClose) {
		this.tcpClose = tcpClose;
	}
	public int getTcpCloseWait() {
		return tcpCloseWait;
	}
	public void setTcpCloseWait(int tcpCloseWait) {
		this.tcpCloseWait = tcpCloseWait;
	}
	public int getTcpLastAck() {
		return tcpLastAck;
	}
	public void setTcpLastAck(int tcpLastAck) {
		this.tcpLastAck = tcpLastAck;
	}
	public int getTcpListen() {
		return tcpListen;
	}
	public void setTcpListen(int tcpListen) {
		this.tcpListen = tcpListen;
	}
	public int getTcpClosing() {
		return tcpClosing;
	}
	public void setTcpClosing(int tcpClosing) {
		this.tcpClosing = tcpClosing;
	}
	public int getTcpIdle() {
		return tcpIdle;
	}
	public void setTcpIdle(int tcpIdle) {
		this.tcpIdle = tcpIdle;
	}
	public int getTcpBound() {
		return tcpBound;
	}
	public void setTcpBound(int tcpBound) {
		this.tcpBound = tcpBound;
	}
	
}
