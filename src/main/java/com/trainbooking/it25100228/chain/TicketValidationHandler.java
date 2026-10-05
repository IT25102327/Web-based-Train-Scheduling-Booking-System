package com.trainbooking.it25100228.chain;

/**
 * Base abstract handler for the Ticket Validation Chain of Responsibility.
 * Defines the successor link and forwarding logic.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
public abstract class TicketValidationHandler {

    protected TicketValidationHandler next;

    /**
     * Sets the next handler in the processing chain.
     *
     * @param next the successor handler
     * @return the successor handler for method chaining
     */
    public TicketValidationHandler setNext(TicketValidationHandler next) {
        this.next = next;
        return next;
    }

    /**
     * Executes the handler's business validation logic.
     *
     * @param context the ticket validation context
     */
    public abstract void handle(TicketValidationContext context);

    /**
     * Passes the context to the next handler if not terminated.
     *
     * @param context the ticket validation context
     */
    protected void passToNext(TicketValidationContext context) {
        if (!context.isHandled() && next != null) {
            next.handle(context);
        }
    }
}
