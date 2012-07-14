import sagex.SageAPI
import sagex.api.AiringAPI
import sagex.remote.rmi.RMISageAPI

import com.google.code.sagetvaddons.sre.engine.DataStore
import com.google.code.sagetvaddons.sre.engine.Version

SageAPI.setProvider(new RMISageAPI('192.168.1.11', 1098))

//new SrePlugin(null).start()

def db = DataStore.getInstance()
println db.getMonitorStatus(2338937)
println db.wasMonitored(AiringAPI.GetAiringForID(2338937))
println Version.get().getVersion()
println Version.get().getSvnPath()
db.newOverride(2338937, 'NHL Hockey', 'Edmonton at San Jose', true)
//db.deleteOverride 2338937