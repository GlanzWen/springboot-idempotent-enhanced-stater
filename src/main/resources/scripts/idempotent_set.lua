-- idempotent_set.lua
local res = redis.call('set', KEYS[1], ARGV[1], 'NX', 'EX', tonumber(ARGV[2]))
if res then return 1 end
return 0
