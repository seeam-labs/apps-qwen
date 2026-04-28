package com.cyber.zenmappro.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cyber.zenmappro.R
import com.cyber.zenmappro.model.HostObject
import com.cyber.zenmappro.model.PortInfo

/**
 * Adapter for displaying scanned hosts in RecyclerView
 */
class HostAdapter(
    private val onHostClick: (HostObject) -> Unit
) : RecyclerView.Adapter<HostAdapter.HostViewHolder>() {

    private var hosts: List<HostObject> = emptyList()
    private val expandedPositions = mutableSetOf<Int>()

    inner class HostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        val tvIpAddress: TextView = itemView.findViewById(R.id.tvIpAddress)
        val tvMacAddress: TextView = itemView.findViewById(R.id.tvMacAddress)
        val tvPortCount: TextView = itemView.findViewById(R.id.tvPortCount)
        val tvLatency: TextView = itemView.findViewById(R.id.tvLatency)
        val btnExpand: ImageView = itemView.findViewById(R.id.btnExpand)
        val expandedDetails: LinearLayout = itemView.findViewById(R.id.expandedDetails)
        val tvOsGuess: TextView = itemView.findViewById(R.id.tvOsGuess)
        val portsContainer: LinearLayout = itemView.findViewById(R.id.portsContainer)
        val tvVendor: TextView = itemView.findViewById(R.id.tvVendor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_host, parent, false)
        return HostViewHolder(view)
    }

    override fun onBindViewHolder(holder: HostViewHolder, position: Int) {
        val host = hosts[position]
        val isExpanded = expandedPositions.contains(position)

        // Set basic info
        holder.tvIpAddress.text = host.ip
        holder.tvMacAddress.text = "MAC: ${host.mac}"
        holder.tvPortCount.text = "${host.portCount} port${if (host.portCount != 1) "s" else ""}"
        holder.tvLatency.text = "${host.latency}ms"
        holder.tvOsGuess.text = "OS: ${host.osGuess}"
        holder.tvVendor.text = "Vendor: ${host.vendor}"

        // Status indicator color based on open ports
        val context = holder.itemView.context
        if (host.hasOpenPorts()) {
            holder.statusIndicator.setBackgroundColor(
                context.getColor(R.color.neon_green)
            )
            holder.tvPortCount.setTextColor(context.getColor(R.color.cyber_pink))
        } else {
            holder.statusIndicator.setBackgroundColor(
                context.getColor(R.color.text_secondary)
            )
            holder.tvPortCount.setTextColor(context.getColor(R.color.text_secondary))
        }

        // Expand/collapse animation
        holder.btnExpand.rotation = if (isExpanded) 180f else 0f
        holder.expandedDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Populate ports list if expanded
        if (isExpanded) {
            populatePortsList(holder.portsContainer, host.openPorts)
        } else {
            holder.portsContainer.removeAllViews()
        }

        // Click listeners
        holder.itemView.setOnClickListener {
            onHostClick(host)
        }

        holder.btnExpand.setOnClickListener {
            toggleExpand(position)
        }
    }

    private fun populatePortsList(container: LinearLayout, ports: List<PortInfo>) {
        container.removeAllViews()
        
        if (ports.isEmpty()) {
            val textView = TextView(container.context).apply {
                text = "No open ports detected"
                setTextColor(container.context.getColor(R.color.text_secondary))
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(8, 4, 8, 4)
            }
            container.addView(textView)
            return
        }

        ports.forEach { port ->
            val portView = TextView(container.context).apply {
                text = "  └─ ${port.port}/${port.protocol} (${port.serviceName})"
                setTextColor(container.context.getColor(R.color.neon_green))
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(8, 4, 8, 4)
            }
            container.addView(portView)
        }
    }

    private fun toggleExpand(position: Int) {
        if (expandedPositions.contains(position)) {
            expandedPositions.remove(position)
            notifyItemChanged(position)
        } else {
            expandedPositions.add(position)
            notifyItemChanged(position)
        }
    }

    fun submitList(newHosts: List<HostObject>) {
        hosts = newHosts
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = hosts.size
}
