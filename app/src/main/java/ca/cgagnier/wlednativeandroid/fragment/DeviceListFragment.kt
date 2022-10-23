package ca.cgagnier.wlednativeandroid.fragment

import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ca.cgagnier.wlednativeandroid.*
import ca.cgagnier.wlednativeandroid.databinding.FragmentDeviceListBinding
import ca.cgagnier.wlednativeandroid.repository.DeviceRepository
import ca.cgagnier.wlednativeandroid.service.DeviceApi


class DeviceListFragment : Fragment(R.layout.fragment_device_list),
    DeviceRepository.DataChangedListener,
    SwipeRefreshLayout.OnRefreshListener {

    private val deviceListAdapter = DeviceListAdapter(getAllNotHidden())
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onResume() {
        super.onResume()
        DeviceRepository.registerDataChangedListener(this)
        deviceListAdapter.replaceItems(getAllNotHidden())
        onRefresh()
    }

    private fun getAllNotHidden(): ArrayList<DeviceItem> {
        return DeviceRepository.getAll().filter { !it.isHidden } as ArrayList
    }

    override fun onPause() {
        super.onPause()
        DeviceRepository.unregisterDataChangedListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        val binding = FragmentDeviceListBinding.inflate(inflater, container,false)
        val layoutManager = LinearLayoutManager(binding.root.context)

        binding.deviceListRecyclerView.adapter = deviceListAdapter
        binding.deviceListRecyclerView.layoutManager = layoutManager
        binding.deviceListRecyclerView.setHasFixedSize(true)

        val dividerItemDecoration = DividerItemDecoration(
            binding.deviceListRecyclerView.context,
            layoutManager.orientation
        )
        binding.deviceListRecyclerView.addItemDecoration(dividerItemDecoration)

        swipeRefreshLayout = binding.swipeRefresh
        swipeRefreshLayout.setOnRefreshListener(this)

        val emptyDataObserver = EmptyDataObserver(binding.deviceListRecyclerView, binding.emptyDataParent)
        deviceListAdapter.registerAdapterDataObserver(emptyDataObserver)

        binding.emptyDataParent.findMyDeviceButton.setOnClickListener {
            openAddDeviceFragment()
        }

        return binding.root;
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home, menu)

        val actionBar = activity?.actionBar

        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_device_add -> {
                openAddDeviceFragment()
                true
            }
            R.id.action_refresh -> {
                swipeRefreshLayout.isRefreshing = true
                onRefresh()
                true
            }
            R.id.action_manage_device -> {
                openManageDevicesFragment()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openAddDeviceFragment() {
        val fragment = DeviceDiscoveryFragment()
        switchContent(R.id.fragment_container_view, fragment)
    }

    private fun openManageDevicesFragment() {
        val fragment = DeviceListManageFragment()
        switchContent(R.id.fragment_container_view, fragment)
    }

    private fun switchContent(id: Int, fragment: Fragment) {
        if (context is MainActivity) {
            val mainActivity = context as MainActivity
            mainActivity.switchContent(id, fragment)
        }
    }

    override fun onItemChanged(item: DeviceItem) {
        deviceListAdapter.itemChanged(item)
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onItemAdded(item: DeviceItem) {
        deviceListAdapter.addItem(item)
    }

    override fun onItemRemoved(item: DeviceItem) {
        deviceListAdapter.removeItem(item)
    }

    override fun onRefresh() {
        for (device in deviceListAdapter.getAllItems()) {
            DeviceApi.update(device)
        }
    }
}