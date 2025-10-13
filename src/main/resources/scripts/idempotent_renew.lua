-- idempotent_renew.lua
if redis.call('get', KEYS[1]) == ARGV[1] then
  redis.call('expire', KEYS[1], tonumber(ARGV[2]))
  return 1
end
return 0
