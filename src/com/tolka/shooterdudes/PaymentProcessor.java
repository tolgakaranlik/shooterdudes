package com.tolka.shooterdudes;

import java.math.BigDecimal;

public class PaymentProcessor {
	protected BigDecimal amount;
	protected String explanation;
	
	public BigDecimal getAmount()
	{
		return amount;
	}
	
	public String getExplanation()
	{
		return explanation;
	}
	
	public void setAmount(BigDecimal amount)
	{
		this.amount = amount;
	}
	
	public void setExplanation(String explanation)
	{
		this.explanation = explanation;
	}
	
	public PaymentProcessor(BigDecimal amount, String explanation)
	{
		this.amount = amount;
		this.explanation = explanation;
	}
}
