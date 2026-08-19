local userCooldownKey = KEYS[1]
local userHourlyKey = KEYS[2]
local ipHourlyKey = KEYS[3]

local cooldownSeconds = tonumber(ARGV[1])
local userHourlyLimit = tonumber(ARGV[2])
local ipHourlyLimit = tonumber(ARGV[3])
local hourlyTtl = tonumber(ARGV[4])

if redis.call('EXISTS', userCooldownKey) == 1 then
    return 1
end

local userCount = tonumber(redis.call('GET', userHourlyKey) or '0')
if userCount >= userHourlyLimit then
    return 2
end

local ipCount = tonumber(redis.call('GET', ipHourlyKey) or '0')
if ipCount >= ipHourlyLimit then
    return 3
end

-- Reserve user cooldown
redis.call('SET', userCooldownKey, '1', 'EX', cooldownSeconds)

-- Increment user hourly counter
userCount = redis.call('INCR', userHourlyKey)
if userCount == 1 then
    redis.call('EXPIRE', userHourlyKey, hourlyTtl)
end

-- Increment hourly ip counter
ipCount = redis.call('INCR', ipHourlyKey)
if ipCount == 1 then
    redis.call('EXPIRE', ipHourlyKey, hourlyTtl)
end

return 0
