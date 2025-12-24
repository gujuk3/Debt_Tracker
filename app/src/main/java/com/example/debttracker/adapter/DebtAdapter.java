package com.example.debttracker.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.debttracker.DebtDetailActivity;
import com.example.debttracker.R;
import com.example.debttracker.database.Debt;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.DebtViewHolder> {
    private List<Debt> debts;
    private Context context;

    public DebtAdapter(Context context, List<Debt> debts) {
        this.context = context;
        this.debts = debts;
    }

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_debt, parent, false);
        return new DebtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        Debt debt = debts.get(position);

        holder.tvPersonName.setText(debt.getPersonName());

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        holder.tvAmount.setText(currencyFormat.format(debt.getAmount()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("tr"));
        holder.tvDate.setText(dateFormat.format(new Date(debt.getDate())));

        if (debt.getDescription() != null && !debt.getDescription().isEmpty()) {
            holder.tvDescription.setText(debt.getDescription());
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        if (debt.isPaid()) {
            holder.tvPersonName.setPaintFlags(holder.tvPersonName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvAmount.setPaintFlags(holder.tvAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.cardView.setAlpha(0.5f);
        } else {
            holder.tvPersonName.setPaintFlags(holder.tvPersonName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvAmount.setPaintFlags(holder.tvAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.cardView.setAlpha(1.0f);
        }

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DebtDetailActivity.class);
            intent.putExtra("DEBT_ID", debt.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return debts.size();
    }

    public void updateDebts(List<Debt> newDebts) {
        this.debts = newDebts;
        notifyDataSetChanged();
    }

    static class DebtViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvPersonName, tvAmount, tvDate, tvDescription;

        public DebtViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}