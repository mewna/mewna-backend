package com.mewna.paypal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.plugin.util.TextureManager;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import io.sentry.Sentry;
import lombok.Getter;
import lombok.Value;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author amy
 * @since 6/30/18.
 */
@SuppressWarnings("unused")
public class PaypalHandler {
    private static final String DOMAIN = System.getenv("DOMAIN");
    private static final String EXTERNAL_API = System.getenv("EXTERNAL_API");
    private static final String REDIR = EXTERNAL_API + "/api/v1/data/store/checkout/confirm";
    private static final String CLIENT = System.getenv("PAYPAL_CLIENT");
    private static final String SECRET = System.getenv("PAYPAL_SECRET");
    // `sandbox` or `live`
    private static final String MODE = System.getenv("PAYPAL_MODE");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private final List<SKU> skus = new ArrayList<>();
    
    public PaypalHandler(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    public void loadBackgroundManifest(final String json) {
        try {
            skus.addAll(MAPPER.readValue(json, new TypeReference<List<SKU>>() {
            }));
            logger.info("Loaded background manifest SKUs: {}", skus);
        } catch(final IOException e) {
            e.printStackTrace();
        }
    }
    
    private boolean validateSku(final String paymentAmount, final String sku) {
        return skus.stream().filter(e -> e.sku.equalsIgnoreCase(sku))
                .anyMatch(e -> paymentAmount.equalsIgnoreCase(String.format("%.2f", e.cents / 100D)));
    }
    
    private Optional<SKU> getSku(final String sku) {
        return skus.stream().filter(e -> e.sku.equalsIgnoreCase(sku)).findFirst();
    }
    
    public JSONObject startPayment(final String userId, final String skuId) {
        final Optional<SKU> sku = getSku(skuId);
        if(sku.isPresent()) {
            final String paymentAmount = String.format("%.2f", sku.get().getCents() / 100D);
    
            final APIContext context = new APIContext(CLIENT, SECRET, MODE);
            final Payer payer = new Payer().setPaymentMethod("paypal");
            
            final RedirectUrls redirectUrls = new RedirectUrls().setCancelUrl(DOMAIN).setReturnUrl(REDIR + "?userId=" + userId);
            final Details details = new Details().setShipping("0").setTax("0").setSubtotal(paymentAmount);
            final Item paymentItem = new Item().setName("Mewna").setCurrency("USD").setPrice(paymentAmount).setQuantity("1").setSku(skuId);
            final ItemList itemList = new ItemList().setItems(new ArrayList<>(Collections.singletonList(paymentItem)));
            
            final Amount amount = new Amount().setCurrency("USD").setTotal(paymentAmount).setDetails(details);
    
            final Transaction transaction = new Transaction();
            transaction.setAmount(amount).setItemList(itemList);
            final List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(transaction);
            
            final Payment payment = new Payment().setIntent("sale").setPayer(payer).setRedirectUrls(redirectUrls)
                    .setTransactions(transactionList);
            
            try {
                if(!validateSku(paymentAmount, skuId)) {
                    throw new IllegalStateException(String.format("%s not valid paymentAmount for %s", paymentAmount, skuId));
                }
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
                Sentry.capture(e);
                return new JSONObject().put("status", "error").put("error", e.getMessage());
            } catch(final IllegalArgumentException e) {
                logger.error("COULDN'T CREATE PAYPAL TRANSACTION:", e);
                e.printStackTrace();
                Sentry.capture(e);
                return new JSONObject().put("status", "error").put("error", e.getMessage());
            }
        } else {
            return new JSONObject().put("status", "error").put("error", "no sku");
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
            
            final Collection<String> skus = new ArrayList<>();
            
            for(final Transaction t : createdPayment.getTransactions()) {
                for(final Item item : t.getItemList().getItems()) {
                    skus.add(item.getSku());
                }
                amount += (long) (Double.parseDouble(t.getAmount().getTotal()) * 100L);
            }
            
            final Optional<Account> maybeAccount = mewna.getAccountManager().getAccountById(userId);
            if(maybeAccount.isPresent()) {
                final Account account = maybeAccount.get();
                final List<String> packs = new ArrayList<>(account.getOwnedBackgroundPacks());
                for(final String skuId : skus) {
                    if(skuId.startsWith("Background-Pack-")) {
                        final String pack = skuId.replace("Background-Pack-", "").toLowerCase();
                        if(TextureManager.getPacks().containsKey(pack)) {
                            if(!packs.contains(pack)) {
                                packs.add(pack);
                            }
                        }
                    }
                }
                account.setOwnedBackgroundPacks(packs);
                mewna.getDatabase().saveAccount(account);
            }
            
            logger.info("Completed Paypal transaction for USD ${}: {}", amount, createdPayment);
            return new JSONObject().put("status", "success");
        } catch(final PayPalRESTException e) {
            logger.info("COULDN'T COMPLETE PAYPAL TRANSACTION:");
            logger.error("{}", e.getDetails());
            e.printStackTrace();
            return new JSONObject().put("status", "error").put("error", e.getMessage());
        }
    }
    
    @Value
    @SuppressWarnings("WeakerAccess")
    public static final class SKU {
        private final String sku;
        private final long cents;
    }
}
