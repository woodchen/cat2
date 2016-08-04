package org.unidal.cat.plugin.transaction.report.page;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;

import org.unidal.cat.core.report.view.svg.GraphBuilder;
import org.unidal.cat.plugin.transaction.TransactionConstants;
import org.unidal.cat.plugin.transaction.filter.TransactionAllNameFilter;
import org.unidal.cat.plugin.transaction.filter.TransactionAllNameGraphFilter;
import org.unidal.cat.plugin.transaction.filter.TransactionAllTypeFilter;
import org.unidal.cat.plugin.transaction.filter.TransactionAllTypeGraphFilter;
import org.unidal.cat.plugin.transaction.filter.TransactionNameFilter;
import org.unidal.cat.plugin.transaction.filter.TransactionNameGraphFilter;
import org.unidal.cat.plugin.transaction.filter.TransactionTypeFilter;
import org.unidal.cat.plugin.transaction.filter.TransactionTypeGraphFilter;
import org.unidal.cat.plugin.transaction.model.entity.TransactionReport;
import org.unidal.cat.plugin.transaction.view.GraphViewModel;
import org.unidal.cat.plugin.transaction.view.NameViewModel;
import org.unidal.cat.plugin.transaction.view.TypeViewModel;
import org.unidal.cat.spi.ReportManager;
import org.unidal.cat.spi.ReportPeriod;
import org.unidal.lookup.annotation.Inject;
import org.unidal.web.mvc.PageHandler;
import org.unidal.web.mvc.annotation.InboundActionMeta;
import org.unidal.web.mvc.annotation.OutboundActionMeta;
import org.unidal.web.mvc.annotation.PayloadMeta;

import com.dianping.cat.Constants;
import com.dianping.cat.mvc.PayloadNormalizer;

public class Handler implements PageHandler<Context> {
	@Inject
	private GraphBuilder m_builder;

	@Inject
	private JspViewer m_jspViewer;

	@Inject
	private PayloadNormalizer m_normalizer;

	@Inject(TransactionConstants.NAME)
	private ReportManager<TransactionReport> m_manager;

	private void handleHistoryGraph(Model model, Payload payload) throws IOException {
		Date startTime = payload.getStartTime();
		String domain = payload.getDomain();
		String ip = payload.getIp();
		String type = payload.getType();
		String name = payload.getName();
		String filterId;

		if (domain.equals(Constants.ALL)) {
			filterId = (name == null ? TransactionAllTypeGraphFilter.ID : TransactionAllNameGraphFilter.ID);
		} else {
			filterId = (name == null ? TransactionTypeGraphFilter.ID : TransactionNameGraphFilter.ID);
		}

		ReportPeriod period = payload.getPeriod();
		TransactionReport current = m_manager.getReport(period, period.getStartTime(startTime), domain, filterId, //
		      "ip", ip, "type", type, "name", name);
		TransactionReport last = m_manager.getReport(period, period.getLastStartTime(startTime), domain, filterId, //
		      "ip", ip, "type", type, "name", name);
		TransactionReport base = m_manager.getReport(period, period.getBaselineStartTime(startTime), domain, filterId, //
		      "ip", ip, "type", type, "name", name);

		GraphViewModel graph = new GraphViewModel(ip, type, name, current, last, base);

		model.setGraph(graph);
		model.setReport(current);
	}

	private void handleHistoryReport(Model model, Payload payload) throws IOException {
		Date startTime = payload.getStartTime();
		String domain = payload.getDomain();
		String ip = payload.getIp();
		String type = payload.getType();
		String sortBy = payload.getSortBy();
		String query = payload.getQuery();
		String filterId;

		if (domain.equals(Constants.ALL)) {
			filterId = (type == null ? TransactionAllTypeFilter.ID : TransactionAllNameFilter.ID);
		} else {
			filterId = (type == null ? TransactionTypeFilter.ID : TransactionNameFilter.ID);
		}

		ReportPeriod period = payload.getPeriod();
		TransactionReport report = m_manager.getReport(period, startTime, domain, filterId, //
		      "ip", ip, "type", type);

		if (report != null) {
			if (type != null) {
				model.setTable(new NameViewModel(report, ip, type, query, sortBy));
			} else {
				model.setTable(new TypeViewModel(report, ip, query, sortBy));
			}
		} else {
			report = new TransactionReport(domain);
			report.setPeriod(period);
			report.setStartTime(startTime);
		}

		model.setReport(report);
	}

	private void handleHourlyGraph(Model model, Payload payload) throws IOException {
		Date startTime = payload.getStartTime();
		String domain = payload.getDomain();
		String ip = payload.getIp();
		String type = payload.getType();
		String name = payload.getName();
		String filterId;

		if (domain.equals(Constants.ALL)) {
			filterId = (name == null ? TransactionAllTypeGraphFilter.ID : TransactionAllNameGraphFilter.ID);
		} else {
			filterId = (name == null ? TransactionTypeGraphFilter.ID : TransactionNameGraphFilter.ID);
		}

		TransactionReport report = m_manager.getReport(ReportPeriod.HOUR, startTime, domain, filterId, //
		      "ip", ip, "type", type, "name", name);

		if (report != null) {
			GraphViewModel graph = new GraphViewModel(m_builder, ip, type, name, report);

			model.setGraph(graph);
		}

		model.setReport(report);
	}

	private void handleHourlyReport(Model model, Payload payload) throws IOException {
		Date startTime = payload.getStartTime();
		String domain = payload.getDomain();
		String ip = payload.getIp();
		String type = payload.getType();
		String sortBy = payload.getSortBy();
		String query = payload.getQuery();
		String filterId;

		if (domain.equals(Constants.ALL)) {
			filterId = (type == null ? TransactionAllTypeFilter.ID : TransactionAllNameFilter.ID);
		} else {
			filterId = (type == null ? TransactionTypeFilter.ID : TransactionNameFilter.ID);
		}

		TransactionReport report = m_manager.getReport(ReportPeriod.HOUR, startTime, domain, filterId, //
		      "ip", ip, "type", type);

		if (report != null) {
			if (type != null) {
				model.setTable(new NameViewModel(report, ip, type, query, sortBy));
			} else {
				model.setTable(new TypeViewModel(report, ip, query, sortBy));
			}
		} else {
			report = new TransactionReport(domain);
			report.setPeriod(ReportPeriod.HOUR);
			report.setStartTime(startTime);
		}

		model.setReport(report);
	}

	@Override
	@PayloadMeta(Payload.class)
	@InboundActionMeta(name = "t")
	public void handleInbound(Context ctx) throws ServletException, IOException {
		// display only, no action here
	}

	@Override
	@OutboundActionMeta(name = "t")
	public void handleOutbound(Context ctx) throws ServletException, IOException {
		Model model = new Model(ctx);
		Payload payload = ctx.getPayload();
		Action action = payload.getAction();

		model.setAction(action);

		switch (action) {
		case REPORT:
			if (payload.getPeriod().isHour()) {
				handleHourlyReport(model, payload);
			} else {
				handleHistoryReport(model, payload);
			}

			break;
		case GRAPH:
			if (payload.getPeriod().isHour()) {
				handleHourlyGraph(model, payload);
			} else {
				handleHistoryGraph(model, payload);
			}

			break;
		}

		TransactionReport report = model.getReport();

		if (report != null) {
			Date startTime = report.getStartTime();
			Date endTime = report.getPeriod().getNextStartTime(startTime);

			report.setEndTime(new Date(endTime.getTime() - 1000));
		}

		if (!ctx.isProcessStopped()) {
			m_jspViewer.view(ctx, model);
		}
	}
}
