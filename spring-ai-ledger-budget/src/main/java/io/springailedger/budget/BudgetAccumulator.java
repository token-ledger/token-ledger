package io.springailedger.budget;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BudgetAccumulator {

  private final ConcurrentHashMap<String, AtomicLong> accumulated
      = new ConcurrentHashMap<>();

  public void add(String serviceId, double costUsd) {
    long microDollars = (long) (costUsd * 1_000_000);
    accumulated
        .computeIfAbsent(serviceId, k -> new AtomicLong(0))
        .addAndGet(microDollars);
  }

  public double getAccumulated(String serviceId) {
    AtomicLong value = accumulated.get(serviceId);
    if (value == null) return 0.0;
    return value.get() / 1_000_000.0;
  }

  public void reset(String serviceId) {
    accumulated.put(serviceId, new AtomicLong(0));
  }
}