-- 대량 더미 데이터 생성 (10만건)
-- H2 데이터베이스의 SYSTEM_RANGE 함수 사용

INSERT INTO users (username, email, age, department, created_at, active)
SELECT
    CONCAT('user_', LPAD(x, 6, '0')), -- user_000001, user_000002...
    CONCAT('user', x, '@company.com'), -- user1@company.com, user2@company.com...
    20 + MOD(x, 40), -- 나이: 20~59 랜덤
    CASE
        WHEN MOD(x, 5) = 0 THEN 'Engineering'
        WHEN MOD(x, 5) = 1 THEN 'Marketing'
        WHEN MOD(x, 5) = 2 THEN 'Sales'
        WHEN MOD(x, 5) = 3 THEN 'HR'
        ELSE 'Finance'
        END, -- 부서 순환
    DATEADD('DAY', -MOD(x, 365), CURRENT_TIMESTAMP), -- 최근 1년간 랜덤 날짜
    CASE WHEN MOD(x, 10) = 0 THEN FALSE ELSE TRUE END -- 10%는 비활성
FROM SYSTEM_RANGE(1, 100000); -- 10만건 생성