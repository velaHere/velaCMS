if redis.call('EXISTS', KEYS[1]) == 0 then
    return -1
end

local attempts = tonumber(
    redis.call('HGET', KEYS[1], 'attempts') or '0'
)

if attempts >= 5 then
    return 0
end

redis.call(
    'HINCRBY',
    KEYS[1],
    'attempts',
    1
)

return 1