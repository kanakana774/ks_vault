DELETE FROM T_SCORE;

-- スコア登録
INSERT INTO T_SCORE (
    SCORE_ID, USER_ID, ANS_NUM_CORRECT, ANS_NUM_WRONG, TIME, SCORE, USER_RANK,
    INSERT_USER_ID, INSERT_DATE, UPDATE_USER_ID, UPDATE_DATE, DELETE_FLG, UPDATE_COUNT
) VALUES
(1, 'user001', 9, 1, 1.000, 100, 'S', 'user001', NOW(), 'user001', NOW(), '0', 0),
(2, 'user001', 9, 1, 22.220, 95, 'S', 'user001', NOW(), 'user001', NOW(), '0', 0),
(3, 'user001', 9, 1, 333.333, 90, 'S', 'user001', NOW(), 'user001', NOW(), '0', 0),
(4, 'user001', 8, 2, 40.000, 82, 'A', 'user001', NOW(), 'user001', NOW(), '0', 0),
(5, 'user001', 5, 5, 50.000, 50, 'C', 'user001', NOW(), 'user001', NOW(), '0', 0),
(6, 'user001', 5, 5, 50.000, 50, 'C', 'user001', NOW(), 'user001', NOW(), '0', 0),
(7, 'user001', 4, 6, 70.000, 40, 'D', 'user001', NOW(), 'user001', NOW(), '0', 0),
(8, 'user001', 3, 7, 80.000, 50, 'C', 'user001', NOW(), 'user001', NOW(), '0', 0),
(9, 'user001', 2, 8, 90.000, 20, 'E', 'user001', NOW(), 'user001', NOW(), '0', 0),
(10, 'user001', 2, 8, 100.000, 20, 'E', 'user001', NOW(), 'user001', NOW(), '0', 0),
(11, 'user001', 2, 8, 100.000, 20, 'E', 'user001', NOW(), 'user001', NOW(), '0', 0),
(12, 'user001', 2, 8, 100.000, 20, 'E', 'user001', NOW(), 'user001', NOW(), '1', 0),
(13, 'user002', 10, 0, 1.000, 100, 'S', 'user002', NOW(), 'user002', NOW(), '0', 0);
