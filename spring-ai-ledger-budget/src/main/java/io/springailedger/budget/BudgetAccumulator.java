package io.springailedger.budget;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// 서비스별 누적 비용을 저장하는 클래스
// "고객지원 챗봇이 오늘 총 얼마 썼는지" 를 기억하는 역할
public class BudgetAccumulator {

  // ConcurrentHashMap = 동시에 여러 요청이 와도 안전하게 처리되는 Map
  // 일반 HashMap은 동시 요청 시 데이터가 꼬일 수 있어서 사용 불가
  // key: serviceId ("svc-customer-support" 같은 서비스명)
  // value: 누적 비용을 마이크로달러(정수)로 저장
  private final ConcurrentHashMap<String, AtomicLong> accumulated
      = new ConcurrentHashMap<>();

  // 비용을 누적하는 메서드
  public void add(String serviceId, double costUsd) {

    // double을 그대로 더하면 소수점 오차 발생
    // ex) 0.1 + 0.2 = 0.30000000000000004 (컴퓨터의 소수점 한계)
    // 그래서 마이크로달러(정수)로 변환해서 저장
    // ex) $0.0045 → 4500으로 저장
    long microDollars = (long) (costUsd * 1_000_000);

    accumulated
        // 해당 serviceId가 없으면 0으로 초기화해서 새로 만들기
        .computeIfAbsent(serviceId, k -> new AtomicLong(0))
        // AtomicLong = 동시 요청에서도 안전하게 숫자를 더할 수 있는 타입
        // 일반 Long은 동시에 더하면 값이 유실될 수 있음
        .addAndGet(microDollars);
  }

  // 현재 누적 비용을 달러로 조회하는 메서드
  public double getAccumulated(String serviceId) {
    AtomicLong value = accumulated.get(serviceId);

    // 아직 이 서비스의 사용 기록이 없으면 0 반환
    if (value == null) return 0.0;

    // 마이크로달러로 저장했으니 다시 달러로 변환해서 반환
    // ex) 4500 → $0.0045
    return value.get() / 1_000_000.0;
  }

  // 누적값을 0으로 초기화하는 메서드
  // 하루가 끝나면 다음날을 위해 리셋할 때 사용
  public void reset(String serviceId) {
    accumulated.put(serviceId, new AtomicLong(0));
  }
}