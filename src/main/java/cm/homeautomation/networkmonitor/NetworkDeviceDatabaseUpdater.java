package cm.homeautomation.networkmonitor;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.LogManager;

import com.google.common.eventbus.Subscribe;

import cm.homeautomation.db.EntityManagerService;
import cm.homeautomation.entities.NetworkDevice;
import cm.homeautomation.eventbus.EventBusService;
import cm.homeautomation.eventbus.EventObject;

public class NetworkDeviceDatabaseUpdater {

	public NetworkDeviceDatabaseUpdater() {
		EventBusService.getEventBus().register(this);
		LogManager.getLogger(this.getClass()).debug("registered NetworkDeviceDatabaseUpdater");
	}

	@Subscribe
	public void handleNetworkDeviceFound(EventObject eventObject) {
		final Object data = eventObject.getData();
		if (data instanceof NetworkScannerHostFoundMessage) {
			final NetworkScannerHostFoundMessage foundHostMessage = (NetworkScannerHostFoundMessage) data;

			final NetworkDevice networkDevice = foundHostMessage.getHost();

			final EntityManager em = EntityManagerService.getNewManager();
			List<NetworkDevice> resultList = null;

			final String mac = networkDevice.getMac();

			LogManager.getLogger(this.getClass()).debug("got device for mac: "+mac);
			
			if (mac != null) {
				resultList = em.createQuery("select n from NetworkDevice n where n.mac=:mac", NetworkDevice.class).setParameter("mac", mac)
						.getResultList();
			}

			if ((resultList == null) || resultList.isEmpty()) {
				resultList = em.createQuery("select n from NetworkDevice n where n.ip=:ip", NetworkDevice.class)
						.setParameter("ip", networkDevice.getIp()).getResultList();
			}

			if ((resultList != null) && !resultList.isEmpty()) {
				
				final NetworkDevice existingNetworkDevice = resultList.get(0);
				LogManager.getLogger(this.getClass()).debug("updating existing entry: "+existingNetworkDevice.getId());
				existingNetworkDevice.setIp(networkDevice.getIp());
				existingNetworkDevice.setHostname(networkDevice.getHostname());
				existingNetworkDevice.setMac(networkDevice.getMac());
				existingNetworkDevice.setLastSeen(new Date());
				em.getTransaction().begin();
				em.merge(existingNetworkDevice);
				em.getTransaction().commit();
				LogManager.getLogger(this.getClass()).debug("done updating existing entry: "+existingNetworkDevice.getId());
			} else {
				// this is a new device, so save it
				LogManager.getLogger(this.getClass()).debug("creating new entry: "+networkDevice.getIp());
				em.getTransaction().begin();
				networkDevice.setLastSeen(new Date());
				em.persist(networkDevice);
				em.getTransaction().commit();
				LogManager.getLogger(this.getClass()).debug("done creating new entry: "+networkDevice.getIp());
			}

			em.close();

		}
	}

}
