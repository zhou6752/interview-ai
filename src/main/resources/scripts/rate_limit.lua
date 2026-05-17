-- Redis Lua 滑动窗口限流脚本
-- KEYS[1]: 限流 key (如 rate:user:123:/api/interview/start)
-- ARGV[1]: 窗口大小（秒）
-- ARGV[2]: 允许的最大请求数

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(redis.call('TIME')[1]) -- 秒级时间戳

-- 移除窗口外的旧请求
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 当前窗口内的请求数
local count = redis.call('ZCARD', key)

if count >= limit then
    return 0 -- 限流
end

-- 记录本次请求
redis.call('ZADD', key, now, now .. '-' .. count)
redis.call('EXPIRE', key, window)

return 1 -- 通过
