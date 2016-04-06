package com.sap.loanserver;

import org.eclipse.jetty.server.Request;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class LoanHandlerTest {

    private LoanHandler classUnderTest;

    @Mock
    private Request baseRequest;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    private ByteArrayOutputStream out;
    private PrintWriter writer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.classUnderTest = new LoanHandler(new FileBasedLoanRepository());
    }

    private void initWriter() throws IOException {
        out = new ByteArrayOutputStream();
        writer = new PrintWriter(out);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    public void incompleteRequest() throws Exception {
        initWriter();
        classUnderTest.handle(null, baseRequest, request, response);

        String response = getMessageText(out, writer);
        assertThat(response, Matchers.containsString("Incorrect parameters provided"));
    }

    @Test
    public void completeLoanApplication() throws Exception {
        initWriter();
        when(request.getParameter("amount")).thenReturn("1000");
        when(request.getParameter("contact")).thenReturn("donald@ducks.com");
        when(request.getParameter("action")).thenReturn(LoanHandler.APPLICATION);

        classUnderTest.handle(null, baseRequest, request, response);

        JSONObject response = getResponseAsJsonObject();

        Assert.assertTrue(response.has("id"));
        assertThat(response.getInt("id"), Matchers.greaterThanOrEqualTo(0));
    }

    @Test
    public void newLoanApplicationIsNotApproved() throws Exception {
        initWriter();
        when(request.getParameter("amount")).thenReturn("1000");
        when(request.getParameter("contact")).thenReturn("donald@ducks.com");
        when(request.getParameter("action")).thenReturn(LoanHandler.APPLICATION);

        classUnderTest.handle(null, baseRequest, request, response);

        JSONObject response = getResponseAsJsonObject();

        initWriter();
        int loanRequestID = response.getInt("id");
        when(request.getParameter("ticketId")).thenReturn(String.valueOf(loanRequestID));
        when(request.getParameter("action")).thenReturn(LoanHandler.FETCH);
        classUnderTest.handle(null, baseRequest, request, this.response);

        response = getResponseAsJsonObject();
        assertThat(response.getInt("applicationNo"), is(loanRequestID));
        assertThat(response.getString("contact"), is("donald@ducks.com"));
        assertThat(response.getInt("amount"), is(1000));
        assertThat(response.getBoolean("approved"), is(false));
    }

    @Test
    public void approveLoanApplication() throws Exception {
        initWriter();
        when(request.getParameter("amount")).thenReturn("1000");
        when(request.getParameter("contact")).thenReturn("donald@ducks.com");
        when(request.getParameter("action")).thenReturn(LoanHandler.APPLICATION);
        classUnderTest.handle(null, baseRequest, request, response);

        JSONObject response = getResponseAsJsonObject();

        initWriter();
        int loanRequestID = response.getInt("id");
        when(request.getParameter("ticketId")).thenReturn(String.valueOf(loanRequestID));
        when(request.getParameter("action")).thenReturn(LoanHandler.APPROVE);
        classUnderTest.handle(null, baseRequest, request, this.response);

        assertThat(response.getInt("id"), Matchers.is(loanRequestID));
    }

    @Test
    public void twoNewLoansApplicationHaveDifferentIDs() throws Exception {
        int firstLoadID, secondLoanID;
        initWriter();
        when(request.getParameter("amount")).thenReturn("1000");
        when(request.getParameter("contact")).thenReturn("donald@ducks.com");
        when(request.getParameter("action")).thenReturn(LoanHandler.APPLICATION);

        classUnderTest.handle(null, baseRequest, request, response);

        JSONObject responseObj = getResponseAsJsonObject();
        firstLoadID = responseObj.getInt("id");
        assertThat(firstLoadID, Matchers.greaterThanOrEqualTo(0));


        initWriter();
        when(request.getParameter("amount")).thenReturn("1000");
        when(request.getParameter("contact")).thenReturn("donald@ducks.com");
        when(request.getParameter("action")).thenReturn(LoanHandler.APPLICATION);

        classUnderTest.handle(null, baseRequest, request, response);
        responseObj = getResponseAsJsonObject();
        secondLoanID = responseObj.getInt("id");
        assertThat(secondLoanID, Matchers.greaterThanOrEqualTo(0));

        assertThat(firstLoadID, Matchers.lessThan(secondLoanID));
    }

    private JSONObject getResponseAsJsonObject() {
        String response = getMessageText(out, writer);
        return new JSONObject(response.trim());
    }

    private String getMessageText(ByteArrayOutputStream out, PrintWriter writer) {
        writer.flush();
        return new String(out.toByteArray());
    }
}