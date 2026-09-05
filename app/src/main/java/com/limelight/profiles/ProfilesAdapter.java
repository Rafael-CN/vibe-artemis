package com.limelight.profiles;

import com.limelight.ui.VidroToast;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.limelight.EditProfileActivity;
import com.limelight.R;

import java.util.List;
import java.util.UUID;

public class ProfilesAdapter extends RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder> {
    private final Context context;
    private final ProfilesManager profilesManager;

    public ProfilesAdapter(Context context) {
        this.context = context;
        this.profilesManager = ProfilesManager.getInstance();
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        List<SettingsProfile> profiles = profilesManager.getProfiles();
        SettingsProfile profile = profiles.get(position);
        SettingsProfile activeProfile = profilesManager.getActive();

        holder.profileName.setText(profile.getName());
        holder.profileTimestamp.setText(DateUtils.getRelativeTimeSpanString(
            profile.getModifiedUtc(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

        boolean isActive = activeProfile != null && activeProfile.getUuid().equals(profile.getUuid());
        holder.profileActive.setChecked(isActive);

        // Set click listeners
        holder.profileActive.setOnClickListener(v -> {
            if (isActive) {
                profilesManager.setActive(null);
                VidroToast.show(context, R.string.profile_manager_deactivated_profile, Toast.LENGTH_SHORT);
            } else {
                profilesManager.setActive(profile.getUuid());
                VidroToast.show(context, context.getString(R.string.profile_manager_activated_profile, profile.getName()), Toast.LENGTH_SHORT);
            }
            profilesManager.save(context);
        });

        holder.editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditProfileActivity.class);
            intent.putExtra("profileUuid", profile.getUuid().toString());
            context.startActivity(intent);
        });

        holder.deleteProfile.setOnClickListener(v -> {
            // O ultimo dialogo cru do app: apagar uma cena saia cinza-de-sistema, com o
            // botao lilas do Material, enquanto as outras tres confirmacoes que a pessoa
            // encontra -- sair da sessao, apagar PC, display virtual -- ja usam o tema Vidro.
            // Apagar uma cena e tao destrutivo quanto apagar um PC e merece a mesma voz.
            AlertDialog dialogoDeApagar = new AlertDialog.Builder(context, R.style.VidroDialogPlatform)
                .setTitle(R.string.profile_manager_delete_profile)
                .setMessage(context.getString(R.string.profile_manager_confirm_profile_deleteion, profile.getName()))
                .setPositiveButton(R.string.profile_manager_delete, (dialog, which) -> {
                    profilesManager.delete(profile.getUuid());
                    profilesManager.save(context);
                    VidroToast.show(context, context.getString(R.string.profile_manager_profile_deleted, profile.getName()), Toast.LENGTH_SHORT);
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
            // Depois do show, porque antes dele os botões não existem. Sem a veste, o
            // "Apagar" saía no lilás do Material — ver UiHelper.vestirBotoesDoDialogo.
            com.limelight.utils.UiHelper.vestirBotoesDoDialogo(dialogoDeApagar, context.getResources());
        });

        // Also make the whole row clickable to edit
        holder.itemView.setOnClickListener(v -> holder.editProfile.performClick());
    }

    @Override
    public int getItemCount() {
        return profilesManager.getProfiles().size();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView profileName;
        TextView profileTimestamp;
        RadioButton profileActive;
        ImageButton editProfile;
        ImageButton deleteProfile;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            profileName = itemView.findViewById(R.id.profileName);
            profileTimestamp = itemView.findViewById(R.id.profileTimestamp);
            profileActive = itemView.findViewById(R.id.profileActive);
            editProfile = itemView.findViewById(R.id.editProfile);
            deleteProfile = itemView.findViewById(R.id.deleteProfile);
        }
    }
}