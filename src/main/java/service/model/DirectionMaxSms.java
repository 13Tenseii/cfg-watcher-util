package service.model;

public class DirectionMaxSms {

    private Long id;
    private Long maxSms;
    private Boolean isBillingActive;

    public DirectionMaxSms() {}

    public DirectionMaxSms(Long id, Long maxSms, Boolean isBillingActive) {
        this.id = id;
        this.maxSms = maxSms;
        this.isBillingActive = isBillingActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMaxSms() {
        return maxSms;
    }

    public void setMaxSms(Long maxSms) {
        this.maxSms = maxSms;
    }

    public Boolean isBillingActive() {
        return isBillingActive;
    }

    public void setBillingActive(Boolean billingActive) {
        isBillingActive = billingActive;
    }
}
