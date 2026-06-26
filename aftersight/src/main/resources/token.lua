local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_interval = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local tokens = tonumber(redis.call('HGET', key, 'tokens'))
local last_refill = tonumber(redis.call('HGET', key, 'lastRefill'))

if tokens == nil or last_refill == nil then
    tokens = capacity
    last_refill = now
end

local elapsed = now - last_refill
local refill_tokens = math.floor(elapsed / refill_interval)

if refill_tokens > 0 then
    tokens = math.min(capacity, tokens + refill_tokens)
    last_refill = last_refill + refill_tokens * refill_interval
end

if tokens < 1 then
    redis.call('HSET', key, 'tokens', tokens)
    redis.call('HSET', key, 'lastRefill', last_refill)
    redis.call('EXPIRE', key, math.ceil(capacity * refill_interval / 1000))
    return 0
end

tokens = tokens - 1
redis.call('HSET', key, 'tokens', tokens)
redis.call('HSET', key, 'lastRefill', last_refill)
redis.call('EXPIRE', key, math.ceil(capacity * refill_interval / 1000))

return 1
