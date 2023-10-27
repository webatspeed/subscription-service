package com.webatspeed.subscription.service;

public enum TemplateName {
  PLEASE_CONFIRM("please-confirm"),
  PLEASE_WAIT("please-wait"),
  PLEASE_APPROVE("please-approve"),
  FIRST_CV("first-cv");

  private final String label;

  TemplateName(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
