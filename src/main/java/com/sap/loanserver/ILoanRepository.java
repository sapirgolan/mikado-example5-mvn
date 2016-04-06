package com.sap.loanserver;

/**
 * Created by I062070 on 06/04/2016.
 */
public interface ILoanRepository {
    LoanApplication fetch(String ticketId);

    Ticket store(LoanApplication application);

    Ticket approve(String ticketId);
}
