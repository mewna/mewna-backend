package com.mewna.paypal;

import com.mewna.Mewna;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author amy
 * @since 6/30/18.
 */
@SuppressWarnings("unused")
public class PaypalHandler {
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final String DOMAIN = System.getenv("DOMAIN");
    private static final String EXTERNAL_API = System.getenv("EXTERNAL_API");
    private static final String REDIR = EXTERNAL_API + "/api/v1/checkout/confirm";
    private static final String CLIENT = System.getenv("PAYPAL_CLIENT");
    private static final String SECRET = System.getenv("PAYPAL_SECRET");
    // `sandbox` or `live`
    private static final String MODE = System.getenv("PAYPAL_MODE");
    
    public PaypalHandler(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    public JSONObject startPayment(final String userId, final String paymentAmount) {
        final APIContext context = new APIContext(CLIENT, SECRET, MODE);
        final Payer payer = new Payer().setPaymentMethod("paypal");
    
        final RedirectUrls redirectUrls = new RedirectUrls().setCancelUrl(DOMAIN).setReturnUrl(REDIR + "?user=" + userId);
        logger.info("Redirect URL: {}", redirectUrls.getReturnUrl());
        logger.info("  Cancel URL: {}", redirectUrls.getCancelUrl());
        final Details details = new Details().setShipping("0").setTax("0").setSubtotal(paymentAmount);
    
        final Item paymentItem = new Item().setName("Mewna").setCurrency("USD").setPrice(paymentAmount).setQuantity("1");
        final ItemList itemList = new ItemList().setItems(new ArrayList<>(Collections.singletonList(paymentItem)));
    
        final Amount amount = new Amount().setCurrency("USD").setTotal(paymentAmount).setDetails(details);
    
        final Transaction transaction = new Transaction();
        transaction.setAmount(amount).setDescription("Donation to REMIA").setItemList(itemList);
        final List<Transaction> transactionList = new ArrayList<>();
        transactionList.add(transaction);
    
        final Payment payment = new Payment().setIntent("sale").setPayer(payer).setRedirectUrls(redirectUrls)
                .setTransactions(transactionList);
    
        try {
            final Payment createdPayment = payment.create(context);
            for(final Links link : createdPayment.getLinks()) {
                if(link.getRel().equalsIgnoreCase("approval_url")) {
                    // REDIRECT USER TO link.getHref()
                    return new JSONObject().put("status", "success").put("redirect", link.getHref());
                }
            }
            return new JSONObject().put("status", "error").put("error", "Couldn't find approval url!?");
        } catch(final PayPalRESTException e) {
            logger.error("COULDN'T CREATE PAYPAL TRANSACTION:");
            logger.error("{}", e.getDetails());
            e.printStackTrace();
            return new JSONObject().put("status", "error").put("error", e.getMessage());
        }
    }
    
    public JSONObject finishPayment(final String userId, final String paymentId, final String payerId) {
        final APIContext context = new APIContext(CLIENT, SECRET, MODE);
        final Payment payment = new Payment();
        payment.setId(paymentId);
    
        final PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);
        try {
            final Payment createdPayment = payment.execute(context, paymentExecution);
            long amount = 0;
            for(final Transaction t : createdPayment.getTransactions()) {
                amount += new Double(Double.parseDouble(t.getAmount().getTotal())).longValue();
            }
            
            // TODO: Actually handle this
            /*
            final User u = remia.getDatabase().getUser(user);
            u.setRems(u.getRems() + amount * 100);
            u.update();
            */
        
            logger.info("Completed Paypal transaction for USD ${}: {}", amount, createdPayment);
            return new JSONObject().put("status", "success");
        } catch(final PayPalRESTException e) {
            logger.info("COULDN'T COMPLETE PAYPAL TRANSACTION:");
            logger.error("{}", e.getDetails());
            e.printStackTrace();
            return new JSONObject().put("status", "error").put("error", e.getMessage());
        }
    }
}
